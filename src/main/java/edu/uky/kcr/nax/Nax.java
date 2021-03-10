/*
 * Copyright 2020 University of Kentucky
 * Kentucky Cancer Registry
 * University of Kentucky Markey Cancer Control Program
 * Markey Cancer Research Informatics Shared Resource Facility
 *
 * Permission is hereby granted, free of charge, to use a copy of this software
 * and associated documentation files (the “Software”) for any non-profit or
 * educational use, including without limitation the right to use, copy, modify,
 * merge, publish, and distribute copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * For any for-profit or other commercial use, potential users should contact:
 * Kentucky Cancer Registry
 * ATTN: Associate Director of Informatics
 * 2365 Harrodsburg Road, Suite A230
 * Lexington, KY 40504-3381
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package edu.uky.kcr.nax;

import edu.uky.kcr.nax.model.Item;
import edu.uky.kcr.nax.model.NaaccrData;
import edu.uky.kcr.nax.model.NaaccrDictionary;
import edu.uky.kcr.nax.model.Patient;
import edu.uky.kcr.nax.model.Tumor;
import groovy.lang.Script;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.dom.DOMConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Processing engine for a Nax run, takes configuration paramaters from a NaxConfig, runs them against an input NAACCR XMl file,
 * and returns a NaxResult to record what happened during the run.
 */
public class Nax
{
	private static final Logger logger = Logger.getLogger(Nax.class.getName());
	public static final int MAX_VALUE_COUNT = 5001;
	private NaxConfig naxConfig = null;

	public static final int GZIP_BUFFER = 64 * 1024;
	private static final int OUTPUT_BUFFER = 1024 * 1024 * 16;
	private static final int INPUT_BUFFER = 64 * 1024;
	private static final int PEEK_BUFFER = 8192;

	private Nax()
	{

	}

	public static Nax newInstance(NaxConfig naxConfig)
	{
		Nax nax = new Nax();
		nax.setNaxConfig(naxConfig);

		return nax;
	}

	public NaxConfig getNaxConfig()
	{
		return naxConfig;
	}

	private void setNaxConfig(NaxConfig naxConfig)
	{
		this.naxConfig = naxConfig;
	}

	public List<NaxResult> process(File inputFile)
	{
		return process(inputFile, null);
	}

	public List<NaxResult> process(
			File inputFile,
			File outputFile)
	{
		List<NaxResult> naxResultList = new ArrayList<>();

		try (FileInputStream fileInputStream = new FileInputStream(inputFile))
		{
			naxResultList.addAll(process(fileInputStream, inputFile.getName(), inputFile.length(), outputFile));
		}
		catch (IOException exception)
		{
			NaxResult naxResult = new NaxResult();
			naxResult.setParsingSuccess(false);
			naxResult.setParsingErrorMessage(exception.getMessage());
			naxResult.setParsingErrorMessageDetails(ExceptionUtils.getStackTrace(exception));
			naxResultList.add(naxResult);
		}

		return naxResultList;
	}

	public List<NaxResult> process(
			InputStream inputStream,
			String name,
			long size)
	{
		return process(inputStream, name, size, null);
	}

	public List<NaxResult> process(
			InputStream inputStream,
			String name,
			long size,
			File outputFile)
	{
		List<NaxResult> naxResultList = new ArrayList<>();

		try
		{
			if (name.endsWith(".zip"))
			{
				ZipOutputStream zipOutputStream = null;

				if (outputFile != null)
				{
					zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
				}

				try (ZipInputStream zipInputStream = new ZipInputStream(inputStream))
				{
					ZipEntry zipEntry = zipInputStream.getNextEntry();

					while (zipEntry != null)
					{
						String zipEntryName = zipEntry.getName();

						//Check file extension of zipentry, skip over zips in zips
						if (zipEntryName.endsWith(".zip") == false)
						{
							//Write to temp file, delete if necessary, add to zip file if good
							File tempFile = File.createTempFile("nax-", ".xml");
							NaxResult naxResult = processSingleFile(zipInputStream, String.format("%s/%s", name, zipEntryName), zipEntry.getSize(), tempFile);

							if (outputFile != null)
							{
								if (naxResult.isOutputFileDeleted() == false)
								{
									zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));

									try (FileInputStream tempFileInputStream = new FileInputStream(tempFile))
									{
										logger.info(String.format("Writing nax output file %s to Zip File: %s", zipEntryName, outputFile
												.getName()));
										IOUtils.copy(tempFileInputStream, zipOutputStream);
									}

									naxResult.setOutputFilename(String.format("%s/%s", outputFile.getName(), zipEntryName));
									naxResult.setOutputFile(null);
								}
								else
								{
									naxResult.setOutputFilename(outputFile.getName());
									naxResult.setOutputFile(null);
								}
							}

							naxResultList.add(naxResult);
							FileUtils.forceDelete(tempFile);
						}

						zipEntry = zipInputStream.getNextEntry();
					}
				}
				finally
				{
					IOUtils.closeQuietly(zipOutputStream);
				}
			}
			else
			{
				NaxResult naxResult = processSingleFile(inputStream, name, size, outputFile);
				naxResultList.add(naxResult);
			}
		}
		catch (Exception exception)
		{
			NaxResult naxResult = new NaxResult();
			naxResult.setParsingSuccess(false);
			naxResult.setParsingErrorMessage(exception.getMessage());
			naxResult.setParsingErrorMessageDetails(ExceptionUtils.getStackTrace(exception));
			naxResultList.add(naxResult);

			exception.printStackTrace();
		}

		return naxResultList;
	}

	private NaxResult processSingleFile(InputStream inputStream,
										String name,
										long size,
										File outputFile)
	{
		NaxResult naxResult = new NaxResult();

		naxResult.setNaxConfig(getNaxConfig());
		naxResult.setOutputFile(outputFile);

		InputStream xmlInputStream = null;
		OutputStream outputStream = null;

		try
		{
			ProgressTrackingDigestInputStream progressTrackingDigestInputStream = new ProgressTrackingDigestInputStream(inputStream, name, size);

			if (name.endsWith(".gz"))
			{
				xmlInputStream = new BufferedInputStream(new GZIPInputStream(progressTrackingDigestInputStream, GZIP_BUFFER), INPUT_BUFFER);
			}
			else
			{
				xmlInputStream = new BufferedInputStream(progressTrackingDigestInputStream, INPUT_BUFFER);
			}

			naxResult.setInputFileInfo(progressTrackingDigestInputStream);

			if (naxResult.getOutputFile() != null)
			{
				if (naxResult.getOutputFile().getName().endsWith(".gz"))
				{
					logger.info(String.format("Output will be compressed to: %s...", naxResult
							.getOutputFilename()));
					outputStream = new BufferedOutputStream(new GZIPOutputStream(
							new FileOutputStream(naxResult.getOutputFile()), GZIP_BUFFER), OUTPUT_BUFFER);
				}
				else
				{
					logger.info(String.format("Output will be uncompressed to: %s...", naxResult
							.getOutputFilename()));
					outputStream = new BufferedOutputStream(
							new FileOutputStream(naxResult.getOutputFile()), OUTPUT_BUFFER);
				}

			}
			else
			{
				outputStream = new NullOutputStream();
			}

			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(outputStream);

			XMLInputFactory xmlInputFactory = XMLInputFactory2.newInstance();
			XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(xmlInputStream);

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();

			DOMConverter domConverter = new DOMConverter();

			int lastPercent = 0;
			NaaccrData naaccrData = new NaaccrData();
			boolean foundNaaccrDataElement = false;

			xmlWriter.writeStartDocument(xmlStreamReader.getCharacterEncodingScheme(), xmlStreamReader
					.getVersion());
			xmlWriter.writeCharacters("\n");

			logger.info(String.format("Start reading %s...", naxResult.getInputFileInfo().getName()));

			while (xmlStreamReader.hasNext())
			{
				int xmlEventType = xmlStreamReader.next();

				switch (xmlEventType)
				{
					case XMLStreamConstants.END_ELEMENT:
					{
						xmlWriter.writeEndElement();

						break;
					}

					case XMLStreamConstants.CHARACTERS:
					{
						xmlWriter.writeCharacters(xmlStreamReader.getText());

						break;
					}

					case XMLStreamConstants.START_ELEMENT:
					{
						QName qName = xmlStreamReader.getName();
						String elementName = qName.getLocalPart();

						switch (elementName)
						{
							case NaxConstants.NAACCR_DATA_ELEMENT:
							{
								foundNaaccrDataElement = true;

								handleStartNaaccrDataElement(naaccrData, naxResult, xmlStreamReader, xmlWriter);

								break;
							}

							//This catches Item elements outside of any Patient elements (children of NaaccrData)
							case NaxConstants.ITEM_ELEMENT:
							{
								Element itemElement = domConverter.buildDocument(xmlStreamReader, documentBuilder)
										.getDocumentElement();

								if (handleStartItemElementChildOfNaaccrData(itemElement, naaccrData, naxResult))
								{
									domConverter.writeFragment(itemElement, xmlWriter);
								}

								break;
							}

							case NaxConstants.PATIENT_ELEMENT:
							{
								Element patientElement = domConverter
										.buildDocument(xmlStreamReader, documentBuilder)
										.getDocumentElement();

								Patient patient = new Patient();
								patient.initialize(patientElement);

								if (includeElementAfterRunningScripts(elementName, naaccrData, patient, null, null, patient
										.getElement(), naxConfig, naxResult.getInputFileInfo().getName()))
								{
									Item[] patientItems = patient.getItems().values().toArray(new Item[]{});

									Map<String, Integer> elementCounts = new HashMap<>();
									Map<String, Integer> excludedElementCounts = new HashMap<>();
									Map<String, Integer> naaccrIdCounts = new HashMap<>();
									Map<String, Integer> excludedNaaccrIdCounts = new HashMap<>();

									for (int i = 0; i < patientItems.length; i++)
									{
										Item patientItem = patientItems[i];

										if (includeItem(
												naxConfig.getIncludedItems(),
												naxConfig.getExcludedItems(),
												patientItem.getNaaccrId(),
												naaccrData.getNaaccrDictionary(),
												naxConfig.getUserDictionaries(),
												naaccrData.getDefaultUserDictionary()) &&
												includeElementAfterRunningScripts(
														NaxConstants.ITEM_ELEMENT,
														naaccrData,
														patient,
														null,
														patientItem,
														patientItem.getItemElement(),
														naxConfig,
														naxResult.getInputFileInfo().getName()))
										{
											//Keep Item
											incrementCount(NaxConstants.ITEM_ELEMENT, elementCounts);
											incrementCount(patientItem.getNaaccrId(), naaccrIdCounts);
											replaceItemValue(naxConfig
																	 .getReplacementMap(), naxConfig
																	 .getConstantValueMap(), patientItem);

											handleNaaccrIdValueCounts(
													naxResult.getInputFileInfo().getName(),
													naaccrData,
													patient,
													null,
													patientItem.getItemElement(),
													patientItem.getNaaccrId(),
													patientItem.getItemValue(),
													naxResult.getNaxConfig(),
													naxResult.getNaxMetrics().getValueCounts());
										}
										else
										{
											incrementCount(NaxConstants.ITEM_ELEMENT, excludedElementCounts);
											incrementCount(patientItem.getNaaccrId(), excludedNaaccrIdCounts);

											Node trailingWhitespaceNode = patientItem.getItemElement()
													.getNextSibling();

											if (trailingWhitespaceNode.getNodeType() == Node.TEXT_NODE)
											{
												trailingWhitespaceNode.setTextContent("");
											}

											patientItem.getItemElement().getParentNode().removeChild(patientItem
																											 .getItemElement());
										}
									}

									Element[] extraElements = patient.getExtraElements().toArray(new Element[]{});

									for (int i = 0; i < extraElements.length; i++)
									{
										Element extraElement = extraElements[i];

										includeOtherNamespaceElement(extraElement, naaccrData, naxResult);
									}

									Tumor[] tumors = patient.getTumors().toArray(new Tumor[]{});

									for (int i = 0; i < tumors.length; i++)
									{
										Tumor tumor = tumors[i];

										if (includeElementAfterRunningScripts(NaxConstants.TUMOR_ELEMENT, naaccrData, patient, tumor, null, tumor
												.getElement(), naxConfig, naxResult.getInputFileInfo().getName()))
										{
											incrementCount(NaxConstants.TUMOR_ELEMENT, elementCounts);

											//Keep Tumor
											Item[] tumorItems = tumor.getItems().values().toArray(new Item[]{});

											for (int j = 0; j < tumorItems.length; j++)
											{
												Item tumorItem = tumorItems[j];

												if (includeItem(naxConfig
																		.getIncludedItems(), naxResult
																		.getNaxConfig()
																		.getExcludedItems(), tumorItem
																		.getNaaccrId(), naaccrData
																		.getNaaccrDictionary(),
																naxConfig.getUserDictionaries(),
																naaccrData.getDefaultUserDictionary()) &&
														includeElementAfterRunningScripts(
																NaxConstants.ITEM_ELEMENT, naaccrData, patient, tumor, tumorItem, tumorItem
																		.getItemElement(),
																naxConfig, naxResult.getInputFileInfo().getName()))
												{
													//Keep Item
													incrementCount(NaxConstants.ITEM_ELEMENT, elementCounts);
													incrementCount(tumorItem.getNaaccrId(), naaccrIdCounts);

													replaceItemValue(naxConfig
																			 .getReplacementMap(), naxConfig
																			 .getConstantValueMap(), tumorItem);

													handleNaaccrIdValueCounts(
															naxResult.getInputFileInfo().getName(),
															naaccrData,
															patient,
															tumor,
															tumorItem.getItemElement(),
															tumorItem.getNaaccrId(),
															tumorItem.getItemValue(),
															naxResult.getNaxConfig(),
															naxResult.getNaxMetrics().getValueCounts());
												}
												else
												{
													incrementCount(NaxConstants.ITEM_ELEMENT, excludedElementCounts);
													incrementCount(tumorItem.getNaaccrId(), excludedNaaccrIdCounts);

													Node trailingWhitespaceNode = tumorItem.getItemElement()
															.getNextSibling();

													if (trailingWhitespaceNode != null && trailingWhitespaceNode
															.getNodeType() == Node.TEXT_NODE)
													{
														trailingWhitespaceNode.setTextContent("");
													}

													tumor.getElement().removeChild(tumorItem.getItemElement());
												}
											}

											Element[] tumorExtraElements = tumor.getExtraElements()
													.toArray(new Element[]{});

											for (int j = 0; j < tumorExtraElements.length; j++)
											{
												Element tumorExtraElement = tumorExtraElements[j];

												includeOtherNamespaceElement(tumorExtraElement, naaccrData, naxResult);
											}

										}
										else
										{
											incrementCount(NaxConstants.TUMOR_ELEMENT, excludedElementCounts);

											Item[] tumorItems = tumor.getItems().values().toArray(new Item[]{});

											for (int j = 0; j < tumorItems.length; j++)
											{
												Item tumorItem = tumorItems[j];
												incrementCount(NaxConstants.ITEM_ELEMENT, excludedElementCounts);
												incrementCount(tumorItem.getNaaccrId(), excludedNaaccrIdCounts);
											}

											Node trailingWhitespaceNode = tumor.getElement().getNextSibling();

											if (trailingWhitespaceNode != null && trailingWhitespaceNode
													.getNodeType() == Node.TEXT_NODE)
											{
												trailingWhitespaceNode.setTextContent("");
											}

											patient.getElement().removeChild(tumor.getElement());
											patient.getTumors().remove(tumor);
										}
									}

									if (patient.getTumors().size() == 0 && naxConfig.isRemoveEmptyPatients())
									{
										incrementCount(elementName, naxResult.getNaxMetrics()
												.getExcludedElementCounts());

										//If we remove the patient due to no Tumors, we need to exclude all of the elements and naaccrIds, not just the ones excluded above
										incrementCounts(excludedElementCounts, naxResult.getNaxMetrics()
												.getExcludedElementCounts());
										incrementCounts(elementCounts, naxResult.getNaxMetrics()
												.getExcludedElementCounts());
										incrementCounts(excludedNaaccrIdCounts, naxResult.getNaxMetrics()
												.getExcludedNaaccrIdCounts());
										incrementCounts(naaccrIdCounts, naxResult.getNaxMetrics()
												.getExcludedNaaccrIdCounts());
									}
									else
									{
										incrementCount(elementName, naxResult.getNaxMetrics().getElementCounts());

										incrementCounts(elementCounts, naxResult.getNaxMetrics()
												.getElementCounts());
										incrementCounts(excludedElementCounts, naxResult.getNaxMetrics()
												.getExcludedElementCounts());
										incrementCounts(naaccrIdCounts, naxResult.getNaxMetrics()
												.getNaaccrIdCounts());
										incrementCounts(excludedNaaccrIdCounts, naxResult.getNaxMetrics()
												.getExcludedNaaccrIdCounts());

										domConverter.writeFragment(patient.getElement(), xmlWriter);

										String patientCountKey = String.format("%d Tumors", patient.getTumors()
												.size());

										if (patient.getTumors().size() == 1)
										{
											patientCountKey = "1 Tumor";
										}

										Integer patientCount = naxResult.getNaxMetrics()
												.getPatientCountsPerTumorCount()
												.getOrDefault(
														patientCountKey, Integer.valueOf(0));
										naxResult.getNaxMetrics().getPatientCountsPerTumorCount().put(
												patientCountKey, Integer.valueOf(patientCount.intValue() + 1));
									}
								}
								else
								{
									incrementCount(elementName, naxResult.getNaxMetrics()
											.getExcludedElementCounts());
								}


								break;
							}

							default:
							{
								if (foundNaaccrDataElement)
								{
									Document document = domConverter
											.buildDocument(xmlStreamReader, documentBuilder);
									Element extraElement = document.getDocumentElement();

									if (includeOtherNamespaceElement(extraElement, naaccrData, naxResult))
									{
										domConverter.writeFragment(extraElement, xmlWriter);
									}
								}
								else
								{
									throw new Exception("Root NaaccrData element not found, XML does not look like NAACCR XML.");
								}

								break;
							}
						}

						break;
					}

					default:
					{
						break;
					}
				}

				if (progressTrackingDigestInputStream.getTotalLength() > -1)
				{
					double percentRead = Math.floor(((float) progressTrackingDigestInputStream
							.getTotalRead() / (float) progressTrackingDigestInputStream.getTotalLength()) * 100f);

					if ((percentRead != lastPercent) && (percentRead % 5 == 0))
					{
						lastPercent = (int) percentRead;
						logger.info(String.format("Read %d%% (%d / %d bytes) of input file...",
												  lastPercent,
												  progressTrackingDigestInputStream.getTotalRead(),
												  progressTrackingDigestInputStream.getTotalLength()));
					}
				}

			}

			xmlWriter.flush();
			xmlWriter.close();

			naxResult.setParsingSuccess(true);
		}
		catch (Exception exception)
		{
			naxResult.setParsingSuccess(false);
			naxResult.setParsingErrorMessage(exception.getMessage());
			naxResult.setParsingErrorMessageDetails(ExceptionUtils.getStackTrace(exception));
		}
		finally
		{
			IOUtils.closeQuietly(outputStream);
		}

		naxResult.getNaxMetrics().markEndTime();

		logger.info(String.format("Done reading %s.", naxResult.getInputFileInfo().getName()));

		if (shouldCleanupOutputFiles(getNaxConfig().getDeleteOutputFiles(), naxResult))
		{
			outputFile.delete();
			naxResult.setOutputFileDeleted(true);
		}

		return naxResult;
	}

	private static boolean shouldCleanupOutputFiles(int deleteOutputFiles,
													NaxResult naxResult)
	{
		boolean shouldDeleteOutput = false;

		if (naxResult.getOutputFile() != null && naxResult.getOutputFile().isFile())
		{
			if (deleteOutputFiles == 1)
			{
				Integer patientCount = naxResult.getNaxMetrics().getElementCounts().get(NaxConstants.PATIENT_ELEMENT);

				if (patientCount == null || patientCount.intValue() == 0)
				{
					logger.info(String.format("Deleting output because patient count was 0."));
					shouldDeleteOutput = true;
				}
			}
			else if (deleteOutputFiles == 2)
			{
				Integer tumorCount = naxResult.getNaxMetrics().getElementCounts().get(NaxConstants.TUMOR_ELEMENT);

				if (tumorCount == null || tumorCount.intValue() == 0)
				{
					logger.info(String.format("Deleting output because tumor count was 0."));
					shouldDeleteOutput = true;
				}
			}
		}

		return shouldDeleteOutput;
	}

	private void incrementCounts(Map<String, Integer> countsSource,
								 Map<String, Integer> countsTarget)
	{
		for (String key : countsSource.keySet())
		{
			Integer count = countsSource.get(key);
			incrementCount(key, countsTarget, count);
		}
	}

	private void handleNaaccrIdValueCounts(
			String inputFilename,
			NaaccrData naaccrData,
			Patient patient,
			Tumor tumor,
			Element xmlElement,
			String naaccrId,
			String itemValue,
			NaxConfig naxConfig,
			Map<String, Map<String, Integer>> valueCountsMap)
	{
		if (naxConfig.getValueCountsSimple().contains(naaccrId))
		{
			incrementCountOrOther(naaccrId, itemValue, valueCountsMap);
		}
		else if (naxConfig.getValueCountsScripts().containsKey(naaccrId))
		{
			Map<String, Script> compiledScriptMap = naxConfig.getValueCountsScripts().get(naaccrId);

			for (String name : compiledScriptMap.keySet())
			{
				Script compiledScript = compiledScriptMap.get(name);

				compiledScript.getBinding().setVariable("inputFilename", inputFilename);
				compiledScript.getBinding().setVariable("elementName", NaxConstants.ITEM_ELEMENT);
				compiledScript.getBinding().setVariable("naaccrData", naaccrData);
				compiledScript.getBinding().setVariable("patient", patient);
				compiledScript.getBinding().setVariable("tumor", tumor);
				compiledScript.getBinding().setVariable("element", xmlElement);
				compiledScript.getBinding().setVariable(NaxConstants.NAACCR_ID, StringUtils
						.defaultString(naaccrId, StringUtils.EMPTY));
				compiledScript.getBinding().setVariable(NaxConstants.ITEM_VALUE, StringUtils
						.defaultString(itemValue, StringUtils.EMPTY));

				if (naaccrId != null)
				{
					compiledScript.getBinding().setVariable(naaccrId, StringUtils
							.defaultString(itemValue, StringUtils.EMPTY));
				}

				logger.finer(String.format("Run script on %s[naaccrId=%s] due to %s", NaxConstants.ITEM_ELEMENT, naaccrId, compiledScript
						.getProperty("name")));

				Object returnValue = compiledScript.run();

				if (naaccrId != null)
				{
					compiledScript.getBinding().setVariable(naaccrId, StringUtils.EMPTY);
				}

				String newItemValue = Objects.toString(returnValue, itemValue);

				incrementCountOrOther(name, newItemValue, valueCountsMap);
			}
		}
	}

	private void incrementCountOrOther(String key,
									   String value,
									   Map<String, Map<String, Integer>> valueCountsMap)
	{
		Map<String, Integer> valueCountsForKey = valueCountsMap.getOrDefault(key, new TreeMap<>());

		if (valueCountsForKey.keySet().size() >= (MAX_VALUE_COUNT - 1))
		{
			incrementCount("Other", valueCountsForKey);
			valueCountsMap.put(key, valueCountsForKey);
		}
		else
		{
			incrementCount(value, valueCountsForKey);
			valueCountsMap.put(key, valueCountsForKey);
		}
	}


	private boolean handleStartItemElementChildOfNaaccrData(
			Element itemElement,
			NaaccrData naaccrData,
			NaxResult naxResult)
	{
		boolean includeItemElement = false;

		Item item = new Item(itemElement);

		if (includeItem(
				naxConfig.getIncludedItems(),
				naxConfig.getExcludedItems(),
				item.getNaaccrId(),
				naaccrData.getNaaccrDictionary(),
				naxConfig.getUserDictionaries(),
				naaccrData.getDefaultUserDictionary()) &&
				includeElementAfterRunningScripts(
						NaxConstants.ITEM_ELEMENT,
						naaccrData,
						null,
						null,
						item,
						itemElement,
						naxConfig,
						naxResult.getInputFileInfo().getName()))
		{
			incrementCount(NaxConstants.ITEM_ELEMENT, naxResult.getNaxMetrics().getElementCounts());
			incrementCount(item.getNaaccrId(), naxResult.getNaxMetrics().getNaaccrIdCounts());

			replaceItemValue(naxConfig
									 .getReplacementMap(), naxConfig
									 .getConstantValueMap(), item);


			handleNaaccrIdValueCounts(
					naxResult.getInputFileInfo().getName(),
					naaccrData,
					null,
					null,
					itemElement,
					item.getNaaccrId(),
					item.getItemValue(),
					naxResult.getNaxConfig(),
					naxResult.getNaxMetrics().getValueCounts());

			includeItemElement = true;
		}
		else
		{
			incrementCount(NaxConstants.ITEM_ELEMENT, naxResult.getNaxMetrics()
					.getExcludedElementCounts());

			incrementCount(item.getNaaccrId(), naxResult.getNaxMetrics()
					.getExcludedNaaccrIdCounts());
		}

		return includeItemElement;
	}

	private void handleStartNaaccrDataElement(
			NaaccrData naaccrData,
			NaxResult naxResult,
			XMLStreamReader xmlStreamReader,
			XMLStreamWriter xmlWriter)
			throws XMLStreamException, IOException, SAXException, ParserConfigurationException
	{
		incrementCount(NaxConstants.NAACCR_DATA_ELEMENT, naxResult.getNaxMetrics().getElementCounts());

		xmlWriter.writeStartElement(NaxConstants.NAACCR_DATA_ELEMENT);

		for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
		{
			xmlWriter.writeAttribute(xmlStreamReader.getAttributeLocalName(i), xmlStreamReader
					.getAttributeValue(i));
			naaccrData.getAttributes().put(xmlStreamReader
												   .getAttributeLocalName(i), xmlStreamReader
												   .getAttributeValue(i));
		}

		naxResult.getNaxMetrics().getNaaccrDataAttributes().putAll(naaccrData.getAttributes());

		naaccrData.setNaaccrDictionary(NaaccrDictionary.createBaseDictionary(naaccrData.getNaaccrVersion()));
		naaccrData.setDefaultUserDictionary(NaaccrDictionary
													.createDefaultUserDictionary(naaccrData.getNaaccrVersion()));

		naxResult.setNaaccrVersion(naaccrData.getNaaccrVersion());

		for (int i = 0; i < xmlStreamReader.getNamespaceCount(); i++)
		{
			if (naxResult.getNaxConfig().isIncludeNamespaces() || StringUtils.isEmpty(xmlStreamReader
																							  .getNamespacePrefix(i)))
			{
				xmlWriter.writeNamespace(xmlStreamReader.getNamespacePrefix(i), xmlStreamReader
						.getNamespaceURI(i));
			}
		}
	}

	private void replaceItemValue(
			Map<String, Map<String, String>> replacementMap,
			Map<String, String> constantValues,
			Item item)
	{
		String newValue = null;

		if (replacementMap != null)
		{
			Map<String, String> valueMap = replacementMap.get(item.getNaaccrId());

			if (valueMap != null)
			{
				newValue = valueMap.get(item.getItemValue());
			}
		}

		if (newValue == null && constantValues != null)
		{
			newValue = constantValues.get(item.getNaaccrId());
		}

		if (newValue != null)
		{
			item.setItemValue(newValue);
		}
	}

	private void incrementCount(
			String key,
			Map<String, Integer> counts)
	{
		incrementCount(key, counts, 1);
	}

	private void incrementCount(
			String key,
			Map<String, Integer> counts,
			int size)
	{
		Integer count = counts.getOrDefault(key, Integer.valueOf(0));
		counts.put(key, Integer.valueOf(count.intValue() + size));
	}

	private void incrementOtherCount(
			String prefix,
			String localName,
			Map<String, Map<String, Integer>> counts)
	{
		String firstKey = StringUtils.defaultString(prefix, "DEFAULT");

		if (counts.get(firstKey) == null)
		{
			counts.put(firstKey, new HashMap<>());
		}

		Map<String, Integer> countMap = counts.get(firstKey);
		Integer count = countMap.getOrDefault(localName, Integer.valueOf(0));
		countMap.put(localName, Integer.valueOf(count.intValue() + 1));
	}

	private boolean includeItem(
			List<String> includedItems,
			List<String> excludedItems,
			String naaccrId,
			NaaccrDictionary baseDictionary,
			List<NaaccrDictionary> userDictionaries,
			NaaccrDictionary defaultUserDictionary)

	{
		boolean includeItem = true;
		Integer naaccrNum = NaaccrDictionary
				.lookupNaaccrNum(naaccrId, baseDictionary, userDictionaries, defaultUserDictionary);

		if (naaccrNum == null)
		{
			logger.warning(String.format("Could not find naaccrNum for %s, User Dictionary may be missing", naaccrId));
		}

		if ((includedItems != null) && includedItems.size() > 0)
		{
			includeItem = (includedItems.contains(naaccrId) || ((naaccrNum != null) && includedItems.contains(naaccrNum
																													  .toString())));
		}
		else if ((excludedItems != null) && (excludedItems.size() > 0))
		{
			includeItem = (excludedItems.contains(naaccrId) == false && ((naaccrNum != null) && excludedItems
					.contains(naaccrNum.toString()) == false));
		}

		return includeItem;
	}

	private String getPrefixedElementName(Element element)
	{
		String elementName = element.getLocalName();

		if (StringUtils.isNotEmpty(element.getPrefix()))
		{
			elementName = String.format("%s:%s", element.getPrefix(), element.getLocalName());
		}

		return elementName;
	}

	private boolean includeOtherNamespaceElement(
			Element element,
			NaaccrData naaccrData,
			NaxResult naxResult)
	{
		boolean include = false;

		String elementName = getPrefixedElementName(element);

		if (naxResult.getNaxConfig()
				.isIncludeNamespaces() &&
				includeElementAfterRunningScripts(
						elementName, naaccrData, null, null, null, element, naxResult.getNaxConfig(), naxResult
								.getInputFileInfo().getName()))
		{
			incrementOtherCount(element.getPrefix(), element.getLocalName(), naxResult.getNaxMetrics()
					.getOtherElementCounts());

			NodeList nodeList = element.getChildNodes();

			for (int i = 0; i < nodeList.getLength(); i++)
			{
				Node node = nodeList.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE)
				{
					Element childElement = (Element) node;

					if (includeOtherNamespaceElement(
							childElement,
							naaccrData,
							naxResult))
					{
						incrementOtherCount(childElement.getPrefix(), childElement.getLocalName(), naxResult
								.getNaxMetrics().getOtherElementCounts());
					}
					else
					{
						incrementOtherCount(childElement.getPrefix(), childElement.getLocalName(), naxResult
								.getNaxMetrics().getExcludedOtherElementCounts());
					}
				}
			}

			include = true;
		}
		else
		{
			incrementOtherCount(element.getPrefix(), element.getLocalName(), naxResult.getNaxMetrics()
					.getExcludedOtherElementCounts());

			logger.fine(String.format("Remove %s", elementName));

			Node trailingWhitespaceNode = element.getNextSibling();

			if (trailingWhitespaceNode != null && trailingWhitespaceNode.getNodeType() == Node.TEXT_NODE)
			{
				trailingWhitespaceNode.setTextContent("");
			}

			element.getParentNode().removeChild(element);
		}

		return include;
	}


	private boolean includeElementAfterRunningScripts(
			String elementName,
			NaaccrData naaccrData,
			Patient patient,
			Tumor tumor,
			Item item,
			Element xmlElement,
			NaxConfig naxConfig,
			String inputFilename)
	{
		boolean includeElement = true;

		if (elementName.equals(NaxConstants.PATIENT_ELEMENT))
		{
			includeElement = includeElementAfterRunningCompiledScripts(
					elementName,
					naaccrData,
					patient,
					tumor,
					item,
					xmlElement,
					naxConfig.getCompiledPatientScripts(),
					inputFilename);
		}
		else if (elementName.equals(NaxConstants.TUMOR_ELEMENT))
		{
			includeElement = includeElementAfterRunningCompiledScripts(
					elementName,
					naaccrData,
					patient,
					tumor,
					item,
					xmlElement,
					naxConfig.getCompiledTumorScripts(),
					inputFilename);
		}
		else if (elementName.equals(NaxConstants.ITEM_ELEMENT) && naxConfig.getCompiledItemScripts()
				.containsKey(item.getNaaccrId()))
		{
			includeElement = includeElementAfterRunningCompiledScripts(
					elementName,
					naaccrData,
					patient,
					tumor,
					item,
					xmlElement,
					naxConfig.getCompiledItemScripts().get(item.getNaaccrId()),
					inputFilename);
		}

		includeElement = includeElement && includeElementAfterRunningCompiledScripts(
				elementName,
				naaccrData,
				patient,
				tumor,
				item,
				xmlElement,
				naxConfig.getCompiledScripts(),
				inputFilename);

		return includeElement;
	}

	private boolean includeElementAfterRunningCompiledScripts(
			String elementName,
			NaaccrData naaccrData,
			Patient patient,
			Tumor tumor,
			Item item,
			Element xmlElement,
			List<Script> compiledScripts,
			String inputFilename)
	{
		boolean includeElement = true;

		String naaccrId = StringUtils.EMPTY;
		String itemValue = StringUtils.EMPTY;

		if (item != null)
		{
			naaccrId = item.getNaaccrId();
			itemValue = item.getItemValue();
		}

		if ((compiledScripts != null) && (compiledScripts.size() > 0))
		{
			for (Script compiledScript : compiledScripts)
			{
				compiledScript.getBinding().setVariable("inputFilename", inputFilename);
				compiledScript.getBinding().setVariable("elementName", elementName);
				compiledScript.getBinding().setVariable("naaccrData", naaccrData);
				compiledScript.getBinding().setVariable("patient", patient);
				compiledScript.getBinding().setVariable("tumor", tumor);
				compiledScript.getBinding().setVariable("item", item);
				compiledScript.getBinding().setVariable("element", xmlElement);
				compiledScript.getBinding().setVariable(NaxConstants.NAACCR_ID, StringUtils
						.defaultString(naaccrId, StringUtils.EMPTY));
				compiledScript.getBinding().setVariable(NaxConstants.ITEM_VALUE, StringUtils
						.defaultString(itemValue, StringUtils.EMPTY));

				if (naaccrId != null)
				{
					compiledScript.getBinding().setVariable(naaccrId, StringUtils
							.defaultString(itemValue, StringUtils.EMPTY));
				}

				logger.finer(String.format("Run script on %s[naaccrId=%s] due to %s", elementName, naaccrId, compiledScript
						.getProperty("name")));

				Object returnValue = compiledScript.run();

				if (naaccrId != null)
				{
					compiledScript.getBinding().removeVariable(naaccrId);
				}

				if (returnValue == null)
				{
					includeElement = true;
				}
				else
				{
					includeElement = (Boolean) returnValue;

					if (includeElement == false)
					{
						logger.fine(String.format("Filter out %s[naaccrId=%s] due to %s", elementName, naaccrId, compiledScript
								.getProperty("name")));
						break;
					}
				}
			}
		}

		return includeElement;
	}
}

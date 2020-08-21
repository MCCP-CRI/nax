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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.uky.kcr.cli.CliParser;
import edu.uky.kcr.cli.CliUtils;
import edu.uky.kcr.cli.DefaultCliAdapter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static edu.uky.kcr.nax.NaxConstants.*;

/**
 * Main entry point, argument parsing, and help text output for the command-line Nax processing application.
 */
public class NaxCommandLineApp
		extends DefaultCliAdapter
{
	private static final Logger logger = Logger.getLogger(Nax.class.getName());

	private static final int MAX_EMAIL_BODY_LENGTH = 1000 * 64;
	private static final int MAX_INPUT_FILES = 1000;

	private NaxConfig naxConfig = null;
	private String outputFileSuffix = StringUtils.EMPTY;
	private String outputFilePrefix = StringUtils.EMPTY;
	private File outputFileOrDirectory = null;
	private File inputFile = null;

	public NaxCommandLineApp()
	{
	}

	public File getInputFile()
	{
		return inputFile;
	}

	public void setInputFile(File inputFile)
	{
		this.inputFile = inputFile;
	}

	public File getOutputFileOrDirectory()
	{
		return outputFileOrDirectory;
	}

	public void setOutputFileOrDirectory(File outputFileOrDirectory)
	{
		this.outputFileOrDirectory = outputFileOrDirectory;
	}

	public String getOutputFilePrefix()
	{
		return outputFilePrefix;
	}

	public void setOutputFilePrefix(String outputFilePrefix)
	{
		this.outputFilePrefix = outputFilePrefix;
	}

	public String getOutputFileSuffix()
	{
		return outputFileSuffix;
	}

	public void setOutputFileSuffix(String outputFileSuffix)
	{
		this.outputFileSuffix = outputFileSuffix;
	}

	public NaxConfig getNaxConfig()
	{
		if (this.naxConfig == null)
		{
			this.naxConfig = new NaxConfig();
		}
		return naxConfig;
	}

	public static void main(String[] args)
			throws Exception
	{
		NaxCommandLineApp naxCommandLineApp = new NaxCommandLineApp();

		CliParser cliParser = new CliParser("nax [OPTIONS] <Input File (.xml, .gz, or .zip)>")
				.withOption(OPT_FILTERPATIENT,
							"filterPatient",
							true,
							"File or inline groovy script called on every Patient element that evaluates to false to exclude the patient from output.")
				.withOption(OPT_FILTERTUMOR, "filterTumor", true,
							"File or inline groovy script called on every Tumor element that evaluates to false to exclude the tumor from output.")
				.withOption(OPT_FILTERITEM, "filterItem", true,
							"File or inline groovy script called on Items specified by a naaccrId using the format: <naaccrId>=<inline script or file> If the script evaluates to false, the item will be excluded from output.")
				.withOption(OPT_SCRIPT, "script", true,
							"File or inline groovy script to filter out elements or make changes to any Patients/Tumors/Item. " + "This script will be called for all Patient, Tumor, Item, and other namespace elements, if the script returns false then the element is excluded from output. " + "If the script returns true or does not have a return value, the element will be included in output. This parameter can be specified more than once.")
				.withOption(OPT_OUTPUTFILE, "outputfile", true, "Output file or directory")
				.withOption(OPT_INCLUDEITEMS,
							"includeItems",
							true,
							"Comma-separated list of naaccrIds to include (ex. nameFirst,socialSecurityNumber). If this argument is specified, all other Items will be removed from output. Takes precedence over excluded items parameter.")
				.withOption(OPT_NAMESPACES, "includeNamespaces", true,
							"Boolean value to include non-NAACCR namespaces, defaults to true unless includedItems is non-blank")
				.withOption(OPT_EXCLUDEITEMS, "excludeItems", true,
							"Comma-separated list of naaccrIds to exclude (ex. nameFirst,socialSecurityNumber). Included items take preference over this parameter.")
				.withOption(OPT_TIMESTAMP, "timestamp", true,
							"When the output file is a directory, boolean value to append a timestamp to output filenames, 'true' by default")
				.withOption(OPT_FILEPREFIX, "fileprefix", true,
							"When the output file is a directory, prefix to append to output file names, empty by default")
				.withOption(OPT_REPLACE, "replace", true,
							"CSV file with replacement values for Items, must have a header with: naaccrId, itemValue, newItemValue")
				.withOption(OPT_CONSTANT, "constant", true,
							"Constant value to set for an Item, using the format: <naaccrId>=<value>. Any values from a CSV replacement file (rpl) take precedence over constant values. This parameter can be specified more than once.")
				.withOption(OPT_METRICS, "metrics", true,
							"Level of metrics logging to output, defaults to 1 (0=none, 1=basic, 2=extended)")
				.withOption(OPT_USERDICTIONARY, "userDictionary", true,
							"User Dictionary File to include when parsing XML. This parameter can be specified more than once.")
				.withOption(OPT_DELETEOUTPUTFILES, "deleteOutputFiles", true,
							"Criteria for deleting output files when they don't have enough data after all of the processing from nax, defaults to 0 (0=never delete, 1=delete if no patients, 2=delete if no tumors)")
				.withOption(OPT_REMOVEEMPTYPATIENTS, "removeEmptyPatients", true,
							"When a Patient has no Tumor records, boolean value to remove the empty Patient record, defaults to 'false'")
				.withOption(OPT_VALUECOUNTS, "valueCounts", true,
							"A comma-separated list of naaccrIds to get a count of all values in a file. For continuous values that require data binning before counts, specify a single naaccrId and a Groovy script to 'bin' the data: <naaccrId>=<Groovy script>. (For example, to get counts of all diagnosis years: -vc dateOfDiagnosis=\"left(dateOfDiagnosis, 4)\". This parameter can be specified more than once.")
				.withOption(OPT_EMAILSUBJECT, "emailSubject", true, "Email Subject line")
				.withOption(OPT_EMAILSMTPHOST,
							"emailSmtpHost",
							true,
							"Email SMTP hostname")
				.withOption(OPT_EMAILFROM, "emailFrom", true, "Email address of sender")
				.withOption(OPT_EMAILTO,
							"emailTo", true,
							"Email address of recipient (can specify multiple times)")
				.withOption(OPT_EMAILUSERNAME, "emailUsername", true, "Email address of sender")
				.withOption(
						OPT_EMAILPASSWORD, "emailPassword", true, "Email address of sender")
				.withOption(
						OPT_EMAILCHECKSERVER, "emailCheckServerIdentity", true, "True/False, Check the server identity of the SMTP Email server when using SSL")
				.withOption(
						OPT_EMAILSMTPPORT, "emailSmtpPort", true, "SMTP Server Port to use")
				.withOption(
						OPT_EMAILSSLONCONNECT, "emailSslOnConnect", true, "Use SSL when connecting to SMTP server")
				.withOption(
						OPT_EMAILSSLSMTPPORT, "emailSslSmtpPort", true, "SMTP SSL Port to use")
				.withOption(
						OPT_EMAILTLSENABLED, "emailTlsEnabled", true, "Enable TLS when connecting to SMTP Server")
				.withOption(
						OPT_EMAILTLSREQUIRED, "emailTlsRequired", true, "Require TLS when connecting to SMTP Server")
				.withListener(naxCommandLineApp);

		if (cliParser.parse(args))
		{
			List<NaxResult> naxResultList = new ArrayList<>();
			Nax nax = Nax.newInstance(naxCommandLineApp.getNaxConfig());

			if (naxCommandLineApp.getInputFile().isDirectory())
			{
				Collection<File> inputFiles = FileUtils.listFiles(
						naxCommandLineApp.getInputFile(), new String[]{"xml", "gz", "zip"}, true);

				if (inputFiles.size() > MAX_INPUT_FILES)
				{
					throw new ParseException(String.format("Input directory contained too many .xml, .gz, and .zip files. Choose a directory with less than %d of those files to process.", MAX_INPUT_FILES));
				}
				else
				{
					for (File currentInputFile : inputFiles)
					{
						String outputFilename = String.format("%s%s%s.%s", naxCommandLineApp.getOutputFilePrefix(),
															  FilenameUtils.getBaseName(currentInputFile.getName()),
															  naxCommandLineApp.getOutputFileSuffix(),
															  FilenameUtils
																	  .getExtension(currentInputFile.getName()));

						File outputFileInDir = null;

						if (naxCommandLineApp.getOutputFileOrDirectory() != null)
						{
							outputFileInDir = new File(naxCommandLineApp
															   .getOutputFileOrDirectory(), outputFilename);
						}

						naxResultList.addAll(nax.process(currentInputFile, outputFileInDir));
					}
				}
			}
			else
			{
				naxResultList.addAll(nax.process(naxCommandLineApp.getInputFile(), naxCommandLineApp.getOutputFileOrDirectory()));
			}

			printResults(nax.getNaxConfig(), naxResultList);

			if (StringUtils.isEmpty(nax.getNaxConfig().getEmailSubject()))
			{
				nax.getNaxConfig().setEmailSubject(String.format("nax Results from processing %s",
																 naxCommandLineApp.getInputFile().getName()));
			}

			handleEmail(nax.getNaxConfig(), naxResultList);
		}
	}

	public static void handleEmail(NaxConfig naxConfig, List<NaxResult> naxResults)
			throws JsonProcessingException, EmailException
	{
		if (	naxConfig.getEmailToList().isEmpty() == false &&
				StringUtils.isNoneEmpty(naxConfig.getEmailSmtpHost(), naxConfig.getEmailFrom()))
		{
			String emailBody = resultsAsString(naxConfig.getMetricsLogging(), naxResults.toArray(new NaxResult[]{}));

			logger.info("Sending email...");

			if (emailBody.length() > MAX_EMAIL_BODY_LENGTH)
			{
				emailBody = String.format("%s ...\n %d characters truncated.",
										  emailBody.substring(0, MAX_EMAIL_BODY_LENGTH),
										   MAX_EMAIL_BODY_LENGTH - emailBody.length());
			}

			Email email = createEmail(naxConfig, emailBody);
			email.send();
		}
	}

	public static void printResults(NaxConfig naxConfig,
									 List<NaxResult> naxResults)
			throws JsonProcessingException
	{
		String resultString = resultsAsString(naxConfig.getMetricsLogging(), naxResults.toArray(new NaxResult[]{}));

		logger.info("NaxResult:");
		System.out.println(resultString);
	}

	@Override
	public void handleParsedOption(String opt,
								   String[] parsedValues,
								   CliParser cliParser)
			throws ParseException
	{
		try
		{
			switch (opt)
			{
				case OPT_OUTPUTFILE:
				{
					File outputFile = CliUtils.convertParsedValue(File.class, parsedValues[0]);

					if (getInputFile().isDirectory() != outputFile.isDirectory())
					{
						throw new ParseException(String.format(
								"Input [%s] and Output [%s] must both be either a directory or a file.", getInputFile()
										.getAbsolutePath(), outputFile.getAbsolutePath()));
					}

					setOutputFileOrDirectory(outputFile);

					break;
				}

				case OPT_EMAILFROM:
				{
					getNaxConfig().withEmailFrom(parsedValues[0]);
					break;
				}

				case OPT_EMAILTO:
				{
					getNaxConfig().withEmailTo(parsedValues);
					break;
				}

				case OPT_EMAILSMTPHOST:
				{
					getNaxConfig().withEmailSmtpHost(parsedValues[0]);
					break;
				}

				case OPT_EMAILSUBJECT:
				{
					getNaxConfig().withEmailSubject(parsedValues[0]);
					break;
				}

				case OPT_EMAILUSERNAME:
				{
					getNaxConfig().withEmailUsername(parsedValues[0]);
					break;
				}

				case OPT_EMAILPASSWORD:
				{
					getNaxConfig().withEmailPassword(parsedValues[0]);
					break;
				}

				case OPT_EMAILTLSREQUIRED:
				{
					getNaxConfig().withEmailStartTlsRequired(Boolean.parseBoolean(parsedValues[0]));
					break;
				}

				case OPT_EMAILTLSENABLED:
				{
					getNaxConfig().withEmailStartTlsEnabled(Boolean.getBoolean(parsedValues[0]));
					break;
				}

				case OPT_EMAILSSLONCONNECT:
				{
					getNaxConfig().withEmailSslOnConnect(Boolean.parseBoolean(parsedValues[0]));
					break;
				}

				case OPT_EMAILSSLSMTPPORT:
				{
					getNaxConfig().withEmailSslSmtpPort(parsedValues[0]);
					break;
				}

				case OPT_EMAILCHECKSERVER:
				{
					getNaxConfig().withEmailSslCheckServerIdentity(Boolean.parseBoolean(parsedValues[0]));
					break;
				}

				case OPT_EMAILSMTPPORT:
				{
					getNaxConfig().withEmailSmtpPort(Integer.parseInt(parsedValues[0]));
					break;
				}

				case OPT_METRICS:
				{
					getNaxConfig().withMetricsLogging(Integer.parseInt(parsedValues[0]));

					break;
				}

				case OPT_DELETEOUTPUTFILES:
				{
					getNaxConfig().withDeleteOutputFiles(Integer.parseInt(parsedValues[0]));

					break;
				}

				case OPT_EXCLUDEITEMS:
				{
					String excludedItemsString = parsedValues[0];

					if (excludedItemsString != null)
					{
						excludedItemsString = StringUtils.deleteWhitespace(excludedItemsString);
						getNaxConfig().withExcludedItems(Arrays.asList(StringUtils.split(excludedItemsString, ',')));
					}

					break;
				}

				case OPT_NAMESPACES:
				{
					String includeNamespacesString = parsedValues[0];

					if (includeNamespacesString == null)
					{
						if (getNaxConfig().getIncludedItems().size() > 0)
						{
							includeNamespacesString = "false";
						}
						else
						{
							includeNamespacesString = "true";
						}
					}

					getNaxConfig().withIncludeNamespaces(Boolean.valueOf(includeNamespacesString));

					break;
				}

				case OPT_VALUECOUNTS:
				{
					for (String valueCountsString : parsedValues)
					{
						int indexOfFirstEquals = valueCountsString.indexOf('=');

						if (indexOfFirstEquals > -1)
						{
							String[] naaccrIdAndName = StringUtils.split(
									valueCountsString.substring(0, indexOfFirstEquals), '/');
							String naaccrId = naaccrIdAndName[0];
							String name = naaccrIdAndName[0];

							if (naaccrIdAndName.length > 1)
							{
								name = naaccrIdAndName[1];
							}

							String scriptString = valueCountsString.substring(indexOfFirstEquals + 1);
							File potentialFile = new File(scriptString);

							if (potentialFile.exists())
							{
								getNaxConfig().withValueCountsScriptFile(naaccrId, name, potentialFile);
							}
							else
							{
								getNaxConfig().withValueCountsScriptString(naaccrId, name, scriptString);
							}
						}
						else
						{
							String[] valueCountArray = StringUtils.split(valueCountsString, ',');

							for (int j = 0; j < valueCountArray.length; j++)
							{
								getNaxConfig().withValueCounts(valueCountArray[j]);
							}
						}
					}

					break;
				}

				case OPT_INCLUDEITEMS:
				{
					String includedItemsString = StringUtils.deleteWhitespace(parsedValues[0]);
					getNaxConfig().withIncludedItems(Arrays.asList(StringUtils.split(includedItemsString, ',')));

					break;
				}

				case OPT_CONSTANT:
				{
					for (String parsedValue : parsedValues)
					{
						String[] constantValueSplit = StringUtils.split(parsedValue, '=');
						getNaxConfig().withConstantValue(constantValueSplit[0], constantValueSplit[1]);
					}

					break;
				}

				case OPT_REPLACE:
				{
					File replacementValuesFile = CliUtils.convertParsedValue(File.class, parsedValues[0]);

					getNaxConfig().withReplacementMapFile(replacementValuesFile);

					break;
				}

				case OPT_USERDICTIONARY:
				{
					List<File> userDictionaryFiles = CliUtils.convertParsedValues(File.class, parsedValues);

					for (File userDictionaryFile : userDictionaryFiles)
					{
						getNaxConfig().withUserDictionary(userDictionaryFile);
					}
					break;
				}

				case OPT_SCRIPT:
				case OPT_FILTERTUMOR:
				case OPT_FILTERPATIENT:
				{
					for (String parsedValue : parsedValues)
					{
						File potentialFile = CliUtils.convertParsedValue(File.class, parsedValue);

						if (potentialFile.exists())
						{
							switch (opt)
							{
								case OPT_SCRIPT:
								{
									getNaxConfig().withScriptFile(potentialFile);
									break;
								}
								case OPT_FILTERTUMOR:
								{
									getNaxConfig().withTumorScriptFile(potentialFile);
									break;
								}
								case OPT_FILTERPATIENT:
								{
									getNaxConfig().withPatientScriptFile(potentialFile);
									break;
								}
							}
						}
						else
						{
							switch (opt)
							{
								case OPT_SCRIPT:
								{
									getNaxConfig().withScriptString(parsedValue);
									break;
								}
								case OPT_FILTERTUMOR:
								{
									getNaxConfig().withTumorScriptString(parsedValue);
									break;
								}
								case OPT_FILTERPATIENT:
								{
									getNaxConfig().withPatientScriptString(parsedValue);
									break;
								}
							}
						}
					}

					break;
				}

				case OPT_REMOVEEMPTYPATIENTS:
				{
					getNaxConfig().withRemoveEmptyPatients(Boolean.parseBoolean(parsedValues[0]));
					break;
				}

				case OPT_FILTERITEM:
				{
					for (String parsedValue : parsedValues)
					{
						String[] itemScriptArgumentArray = StringUtils.split(parsedValue, '=');
						String naaccrId = itemScriptArgumentArray[0];
						File potentialFile = new File(itemScriptArgumentArray[1]);

						if (potentialFile.exists())
						{
							getNaxConfig().withItemScriptFile(naaccrId, potentialFile);
						}
						else
						{
							getNaxConfig().withItemScriptString(naaccrId, itemScriptArgumentArray[1]);
						}
					}

					break;
				}

				case OPT_TIMESTAMP:
				{
					boolean includeTimestamp = Boolean.parseBoolean(parsedValues[0]);

					if (includeTimestamp)
					{
						setOutputFileSuffix(DateFormatUtils.format(System.currentTimeMillis(), "_yyyyMMdd_Hmmss"));
					}

					break;
				}

				case OPT_FILEPREFIX:
				{
					setOutputFilePrefix(parsedValues[0]);
					break;
				}
			}
		}
		catch (Exception exception)
		{
			throw new ParseException(String.format("Could not parse option %s: %s ",
												   cliParser.getOption(opt).getLongOpt(),
												   exception.getMessage()));
		}

	}

	@Override
	public void handleNoArguments(CliParser cliParser)
			throws ParseException
	{
		throw new ParseException("A single input file or directory must be specified.");
	}

	@Override
	public void handleParsedArgumentList(List<String> argList,
										 CliParser cliParser)
			throws ParseException
	{
		File inputFile = CliUtils.convertParsedValue(File.class, cliParser.getNonOptionArgs()[0]);

		if (inputFile.exists() == false)
		{
			throw new ParseException("Input file or directory does not exist: " + cliParser.getNonOptionArgs()[0]);
		}

		logger.info(String.format("Input file is: %s", inputFile.getAbsolutePath()));

		setInputFile(inputFile);
	}

	@Override
	public void handleMissingOptions(List<Option> emptyOptions,
									 CliParser cliParser)
			throws ParseException
	{
		if (StringUtils.isAllEmpty(cliParser.getParsedValue("emailSmtpHost"), cliParser.getParsedValue("emailFrom"),
								   cliParser.getParsedValue("emailTo")) == false && StringUtils.isAnyEmpty(
				cliParser.getParsedValue("emailSmtpHost"), cliParser.getParsedValue("emailFrom"),
				cliParser.getParsedValue("emailTo")))
		{
			throw new ParseException(
					"If any email settings are specified, the following minimum email settings must also be specified: emailSmtpHost, emailFrom, emailTo");
		}
	}

	private static Email createEmail(NaxConfig naxConfig,
									 String messageBody)
			throws EmailException
	{
		Email email = new SimpleEmail();

		email.setHostName(naxConfig.getEmailSmtpHost());
		email.setFrom(naxConfig.getEmailFrom());

		email.setSubject(naxConfig.getEmailSubject());

		email.addTo(naxConfig.getEmailToList().toArray(new String[]{}));

		if (StringUtils.isNoneEmpty(naxConfig.getEmailUsername(),
									naxConfig.getEmailPassword()))
		{
			email.setAuthentication(naxConfig.getEmailUsername(),
									naxConfig.getEmailPassword());
		}

		if (naxConfig.getEmailSmtpPort() != null)
		{
			email.setSmtpPort(naxConfig.getEmailSmtpPort());
		}

		if (naxConfig.getEmailSslCheckServerIdentity() != null)
		{
			email.setSSLCheckServerIdentity(naxConfig.getEmailSslCheckServerIdentity());
		}

		if (naxConfig.getEmailStartTlsEnabled() != null)
		{
			email.setStartTLSEnabled(naxConfig.getEmailStartTlsEnabled());
		}

		if (naxConfig.getEmailStartTlsRequired() != null)
		{
			email.setStartTLSRequired(naxConfig.getEmailStartTlsRequired());
		}

		if (naxConfig.getEmailSslOnConnect() != null)
		{
			email.setSSLOnConnect(naxConfig.getEmailSslOnConnect());
		}

		if (naxConfig.getEmailSslSmtpPort() != null)
		{
			email.setSslSmtpPort(naxConfig.getEmailSslSmtpPort());
		}

		email.setMsg(messageBody);

		return email;
	}


	private static String resultsAsString(int metricsLogging,
										  NaxResult... naxResults)
			throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

		for (NaxResult naxResult : naxResults)
		{
			if (metricsLogging == 0)
			{
				naxResult.setNaxMetrics(null);
			}

			if (metricsLogging == 1)
			{
				naxResult.getNaxMetrics().setOtherElementCounts(null);
				naxResult.getNaxMetrics().setExcludedOtherElementCounts(null);
				naxResult.getNaxMetrics().setNaaccrIdCounts(null);
				naxResult.getNaxMetrics().setExcludedNaaccrIdCounts(null);
			}
		}

		return objectMapper.writer().withoutAttribute("").writeValueAsString(naxResults);
	}

}

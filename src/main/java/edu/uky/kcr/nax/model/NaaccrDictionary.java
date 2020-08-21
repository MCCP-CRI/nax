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

package edu.uky.kcr.nax.model;

import edu.uky.kcr.nax.NaxConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A lightweight container for a NaaccrDictionary, built from a classpath resource File.
 * Provides easy access to a naaccrId to naaccrNum HashMap and a naaccrNum to naaccrId HashMap for going between the two IDs.
 */
public class NaaccrDictionary
{

	private String naaccrVersion = null;
	private String dictionaryUri = null;
	private Map<String, Integer> naaccrNumMap = new HashMap<>();
	private Map<Integer, String> naaccrIdMap = new HashMap<>();

	protected NaaccrDictionary()
	{

	}

	public static Integer lookupNaaccrNum(
			String naaccrId,
			NaaccrDictionary baseDictionary,
			List<NaaccrDictionary> userDictionaries,
			NaaccrDictionary defaultUserDictionary)
	{
		Integer naaccrNum = baseDictionary.getNaaccrNumMap().get(naaccrId);

		if (naaccrNum == null)
		{
			if ((userDictionaries == null) || userDictionaries.size() == 0)
			{
				naaccrNum = defaultUserDictionary.getNaaccrNumMap().get(naaccrId);
			}
			else
			{
				for (NaaccrDictionary userDictionary : userDictionaries)
				{
					naaccrNum = userDictionary.getNaaccrNumMap().get(naaccrId);
					if (naaccrNum != null)
					{
						break;
					}
				}
			}
		}

		return naaccrNum;
	}

	public static NaaccrDictionary createBaseDictionary(String naaccrVersion)
			throws ParserConfigurationException, IOException, SAXException
	{
		NaaccrDictionary naaccrDictionary = new NaaccrDictionary();

		naaccrDictionary.setNaaccrVersion(naaccrVersion);
		naaccrDictionary.initialize(NaaccrDictionary.class.getResourceAsStream(String.format("/base-dictionary-%s.xml", naaccrVersion)));

		return naaccrDictionary;
	}

	public static NaaccrDictionary createDefaultUserDictionary(String naaccrVersion)
			throws ParserConfigurationException, IOException, SAXException
	{
		NaaccrDictionary naaccrDictionary = new NaaccrDictionary();

		naaccrDictionary.setNaaccrVersion(naaccrVersion);
		naaccrDictionary.initialize(NaaccrDictionary.class.getResourceAsStream(String.format("/default-user-dictionary-%s.xml", naaccrVersion)));

		return naaccrDictionary;
	}

	public static NaaccrDictionary createUserDictionary(InputStream userDictionaryInputStream)
			throws IOException, ParserConfigurationException, SAXException
	{
		NaaccrDictionary naaccrDictionary = new NaaccrDictionary();

		naaccrDictionary.initialize(userDictionaryInputStream);

		return naaccrDictionary;
	}

	protected void initialize(InputStream inputStream)
			throws ParserConfigurationException, IOException, SAXException
	{
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new InputSource(inputStream));

		NodeList itemDefNodes = document.getElementsByTagName(NaxConstants.ITEM_DEF);
		for (int i = 0; i < itemDefNodes.getLength(); i++)
		{
			Element itemDef = (Element) itemDefNodes.item(i);
			String naaccrId = itemDef.getAttributeNode(NaxConstants.NAACCR_ID).getValue();
			Integer naaccrNum = Integer.valueOf(itemDef.getAttributeNode(NaxConstants.NAACCR_NUM).getValue());

			getNaaccrNumMap().put(naaccrId, naaccrNum);
			getNaaccrIdMap().put(naaccrNum, naaccrId);
		}
	}

	public Map<String, Integer> getNaaccrNumMap()
	{
		return naaccrNumMap;
	}

	public Map<Integer, String> getNaaccrIdMap()
	{
		return naaccrIdMap;
	}

	public String getNaaccrVersion()
	{
		return naaccrVersion;
	}

	public void setNaaccrVersion(String naaccrVersion)
	{
		this.naaccrVersion = naaccrVersion;
	}

	public String getDictionaryUri()
	{
		return dictionaryUri;
	}

	public void setDictionaryUri(String dictionaryUri)
	{
		this.dictionaryUri = dictionaryUri;
	}
}

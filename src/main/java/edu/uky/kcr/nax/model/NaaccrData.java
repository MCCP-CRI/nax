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

import edu.uky.kcr.nax.NaaccrConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Container for NaaccrData NAACCR XML element that can read NAACCR Version as an integer from the baseDictionaryUri attribute and has a HashMap of Item objects keyed by their naaccrId.
 * <br/>
 * NOTE: This class should not inherit from ElementItemContainer, even though NaaccrData has Item elements. The reason is that ElementItemContainer contains a deserialized DOM object
 * and NaaccrData is the root level DOM object in a NAACCR XML Document, so deserializing it would read the entire XML Document into memory,
 * which is not a good idea for large documents.
 */
public class NaaccrData
{
	private static final Logger logger = Logger.getLogger(NaaccrData.class.getName());
	private Map<String, Item> items = new LinkedHashMap<>();
	private Map<String, String> attributes = new LinkedHashMap<>();
	private NaaccrDictionary naaccrDictionary = null;
	private NaaccrDictionary defaultUserDictionary = null;

	public NaaccrData()
	{

	}

	public String getNaaccrVersion()
	{
		return parseVersionString(getAttributes());
	}

	private String parseVersionString(Map<String, String> attributes)
	{
		return StringUtils.split(StringUtils.split(getAttributes().get(NaaccrConstants.BASE_DICTIONARY_URI), '-')[2], '.')[0];
	}

	public Map<String, String> getAttributes()
	{
		return attributes;
	}

	public Map<String, Item> getItems()
	{
		return items;
	}

	public NaaccrDictionary getNaaccrDictionary()
	{
		return naaccrDictionary;
	}

	public void setNaaccrDictionary(NaaccrDictionary naaccrDictionary)
	{
		this.naaccrDictionary = naaccrDictionary;
	}

	public NaaccrDictionary getDefaultUserDictionary()
	{
		return defaultUserDictionary;
	}

	public void setDefaultUserDictionary(NaaccrDictionary defaultUserDictionary)
	{
		this.defaultUserDictionary = defaultUserDictionary;
	}
}

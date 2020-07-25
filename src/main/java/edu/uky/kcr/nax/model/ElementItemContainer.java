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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Container for NAACCR XML Elements that contain Item child elements such as Patient and Tumor elements.
 * <br/>
 * Provides easy access to the underlying DOM Element, a HashMap of child Items where the keys are naaccrIds,
 * and any "extra" DOM element children that are extensions to the NAACCR XML standard with their own namespace.
 */
public abstract class ElementItemContainer
{
	private final Logger logger = Logger.getLogger(ElementItemContainer.class.getName());

	private Element element = null;
	private Map<String, Item> items = null;
	private List<Element> extraElements = null;

	public ElementItemContainer()
	{

	}

	public void initialize(Element element)
	{
		this.element = element;

		NodeList childNodes = getElement().getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++)
		{
			Node childNode = childNodes.item(i);

			if (childNode.getNodeType() == Node.ELEMENT_NODE)
			{
				if (StringUtils.isEmpty(childNode.getPrefix()))
				{
					if (childNode.getLocalName().equals("Item"))
					{
						String naaccrId = childNode.getAttributes().getNamedItem(NaaccrConstants.NAACCR_ID).getNodeValue();

						getItems().put(naaccrId, new Item((Element) childNode));
					}
					else
					{
						initializeNonItemChildElement((Element) childNode);
					}
				}
				else
				{
					initializeExtraElement((Element) childNode);
				}
			}
		}
	}

	public void initializeExtraElement(Element childElement)
	{
		getExtraElements().add(childElement);
	}

	public void initializeNonItemChildElement(Element childElement)
	{

	}

	public Element getElement()
	{
		return element;
	}

	public Map<String, Item> getItems()
	{
		if (this.items == null)
		{
			this.items = new LinkedHashMap<>();
		}

		return this.items;
	}

	public String getItemValue(String naaccrId)
	{
		String returnValue = StringUtils.EMPTY;

		Item item = getItems().get(naaccrId);

		if (item != null)
		{
			returnValue = item.getItemValue();
		}

		return returnValue;
	}

	public int getItemInt(String naaccrId)
	{
		int returnValue = -1;

		Item item = getItems().get(naaccrId);

		if (item != null)
		{
			String stringValue = item.getItemValue();

			try
			{
				returnValue = Integer.parseInt(stringValue);
			}
			catch (NumberFormatException numberFormatException)
			{
				logger.fine(String.format("Could not parse int from naaccrrId %s with value: [%s]", naaccrId, stringValue));
			}
		}

		return returnValue;
	}

	public List<Element> getExtraElements()
	{
		if (this.extraElements == null)
		{
			this.extraElements = new ArrayList<>();
		}

		return extraElements;
	}

	public void insertNewItemElement(Element newItemElement)
	{
		getElement().appendChild(newItemElement);
	}

	public void addItem(String naaccrId,
						Integer naaccrNum,
						String itemValue)
			throws XMLStreamException
	{
		logger.finer(String.format("add Item: %s(%s)=%s", naaccrId, naaccrNum, itemValue));

		if (getElement() != null)
		{
			if (getItems().get(naaccrId) != null)
			{
				throw new XMLStreamException("Item already exists: " + naaccrId);
			}

			Element newItemElement = getElement().getOwnerDocument().createElement("Item");
			newItemElement.setAttribute("naaccrId", naaccrId);
			newItemElement.setAttribute("naaccrNum", naaccrNum.toString());
			newItemElement.setTextContent(itemValue);

			insertNewItemElement(newItemElement);

			getItems().put(naaccrId, new Item(newItemElement));
		}
	}

}

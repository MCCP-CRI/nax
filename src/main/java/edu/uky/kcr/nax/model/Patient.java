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

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for NAACCR XML Patient element, a simple extension to the ElementItemContainer that has a List of Tumor objects
 */
public class Patient
		extends ElementItemContainer
{
	private List<Tumor> tumors = null;

	public Patient()
	{
		super();
	}

	/**
	 * Check every new non-Item child to see if it is a Tumor element that we want to add to the Tumor List
	 * @param childElement
	 */
	@Override
	public void initializeNonItemChildElement(Element childElement)
	{
		if (childElement.getLocalName().equals("Tumor"))
		{
			Tumor tumor = new Tumor();
			tumor.initialize(childElement);
			getTumors().add(tumor);
		}
	}

	/**
	 * When adding new Item elements to a Patient, do it before the first Tumor
	 * @param newItemElement
	 */
	@Override
	public void insertNewItemElement(Element newItemElement)
	{
		if (getTumors().size() > 0)
		{
			getElement().insertBefore(newItemElement, getTumors().get(0).getElement());
		}
		else
		{
			getElement().appendChild(newItemElement);
		}
	}

	public List<Tumor> getTumors()
	{
		if (this.tumors == null)
		{
			this.tumors = new ArrayList<>();
		}

		return this.tumors;
	}
}

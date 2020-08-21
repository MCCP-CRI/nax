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

public class NaxConstants
{
	//NAACCR XML Constants
	public static final String NAACCR_ID = "naaccrId";
	public static final String BASE_DICTIONARY_URI = "baseDictionaryUri";
	public static final String ITEM_DEF = "ItemDef";
	public static final String NAACCR_NUM = "naaccrNum";
	public static final String ITEM_VALUE = "itemValue";
	public static final String NEW_ITEM_VALUE = "newItemValue";
	public static final String PATIENT_ELEMENT = "Patient";
	public static final String ITEM_ELEMENT = "Item";
	public static final String TUMOR_ELEMENT = "Tumor";
	public static final String NAACCR_DATA_ELEMENT = "NaaccrData";

	//Command Line Option Constants
	public static final String OPT_FILTERPATIENT = "fp";
	public static final String OPT_FILTERTUMOR = "ft";
	public static final String OPT_FILTERITEM = "fi";
	public static final String OPT_SCRIPT = "s";
	public static final String OPT_OUTPUTFILE = "o";
	public static final String OPT_NAMESPACES = "ns";
	public static final String OPT_EXCLUDEITEMS = "e";
	public static final String OPT_INCLUDEITEMS = "i";
	public static final String OPT_TIMESTAMP = "ts";
	public static final String OPT_FILEPREFIX = "pre";
	public static final String OPT_REPLACE = "rpl";
	public static final String OPT_CONSTANT = "con";
	public static final String OPT_METRICS = "met";
	public static final String OPT_USERDICTIONARY = "usr";
	public static final String OPT_DELETEOUTPUTFILES = "del";
	public static final String OPT_REMOVEEMPTYPATIENTS = "rep";
	public static final String OPT_VALUECOUNTS = "vc";
	public static final String OPT_EMAILSUBJECT = "emsub";
	public static final String OPT_EMAILFROM = "emfrom";
	public static final String OPT_EMAILTO = "emto";
	public static final String OPT_EMAILSMTPHOST = "emhost";
	public static final String OPT_EMAILSMTPPORT = "emport";
	public static final String OPT_EMAILCHECKSERVER = "emchk";
	public static final String OPT_EMAILSSLSMTPPORT = "emsslport";
	public static final String OPT_EMAILSSLONCONNECT = "emssl";
	public static final String OPT_EMAILTLSENABLED = "emtls";
	public static final String OPT_EMAILTLSREQUIRED = "emtlsreq";
	public static final String OPT_EMAILUSERNAME = "emuser";
	public static final String OPT_EMAILPASSWORD = "empass";
}

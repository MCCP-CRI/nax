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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Container for all results from a Nax processing run, including the config object and file used as input to the run.
 */
public class NaxResult
{
	private NaxMetrics naxMetrics = new NaxMetrics();
	private NaxConfig naxConfig = null;
	@JsonIgnore
	private File outputFile = null;

	private String naaccrVersion = null;

	private NaxFileInfo inputFileInfo = null;

	private boolean parsingSuccess = false;
	private String parsingErrorMessage = null;
	private String parsingErrorMessageDetails = null;

	public NaxResult()
	{

	}

	public void setNaxMetrics(NaxMetrics naxMetrics)
	{
		this.naxMetrics = naxMetrics;
	}

	public void setNaxConfig(NaxConfig naxConfig)
	{
		this.naxConfig = naxConfig;
	}

	public NaxConfig getNaxConfig()
	{
		return naxConfig;
	}

	public NaxMetrics getNaxMetrics()
	{
		return naxMetrics;
	}

	public String getNaaccrVersion()
	{
		return naaccrVersion;
	}

	public void setNaaccrVersion(String naaccrVersion)
	{
		this.naaccrVersion = naaccrVersion;
	}

	@JsonProperty("inputFile")
	public NaxFileInfo getInputFileInfo()
	{
		return this.inputFileInfo;
	}

	public void setInputFileInfo(NaxFileInfo inputFileInfo)
	{
		this.inputFileInfo = inputFileInfo;
	}

	public String getOutputFilename()
	{
		String outputFilename = StringUtils.EMPTY;

		if (getOutputFile() != null)
		{
			outputFilename = getOutputFile().getAbsolutePath();
		}

		return outputFilename;
	}

	public File getOutputFile()
	{
		return outputFile;
	}

	public void setOutputFile(File outputFile)
	{
		this.outputFile = outputFile;
	}

	public boolean isParsingSuccess()
	{
		return parsingSuccess;
	}

	public void setParsingSuccess(boolean parsingSuccess)
	{
		this.parsingSuccess = parsingSuccess;
	}

	public String getParsingErrorMessage()
	{
		return parsingErrorMessage;
	}

	public void setParsingErrorMessage(String parsingErrorMessage)
	{
		this.parsingErrorMessage = parsingErrorMessage;
	}

	public String getParsingErrorMessageDetails()
	{
		return parsingErrorMessageDetails;
	}

	public void setParsingErrorMessageDetails(String parsingErrorMessageDetails)
	{
		this.parsingErrorMessageDetails = parsingErrorMessageDetails;
	}
}

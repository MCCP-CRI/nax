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
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * This class contains counts of NAACCR XML elements, naaccrIds, and total runtime from a Nax processing run.
 * It will typically be returned inside a {@link NaxResult} object, and then ouput as JSON text to stdout.
 * <br/>
 * The data in this object can be used to get a basic inventory of what is in a NAACCR XML file such as number of
 * Patients, Tumors, and Items.
 */
public class NaxMetrics
{
	@JsonIgnore
	private Long startTimeMillis = null;
	@JsonIgnore
	private Long endTimeMillis = null;

	private String duration = null;
	private String startTime = null;
	private String endTime = null;

	private Map<String, String> naaccrDataAttributes = new TreeMap<>();

	private Map<String, Integer> excludedElementCounts = new TreeMap<>();

	private Map<String, Map<String, Integer>> excludedOtherElementCounts = new TreeMap<>();

	private Map<String, Integer> elementCounts = new TreeMap<>();

	private Map<String, Integer> patientCountsPerTumorCount = new TreeMap<>();

	private Map<String, Map<String, Integer>> otherElementCounts = new TreeMap<>();

	private Map<String, Integer> naaccrIdCounts = new TreeMap<>();

	private Map<String, Map<String, Integer>> valueCounts = new TreeMap<>();

	private Map<String, Integer> excludedNaaccrIdCounts = new TreeMap<>();

	public Map<String, Integer> getElementCounts()
	{
		return elementCounts;
	}

	public Map<String, Integer> getPatientCountsPerTumorCount()
	{
		return patientCountsPerTumorCount;
	}

	public void setNaaccrIdCounts(Map<String, Integer> naaccrIdCounts)
	{
		this.naaccrIdCounts = naaccrIdCounts;
	}

	public void setExcludedNaaccrIdCounts(Map<String, Integer> excludedNaaccrIdCounts)
	{
		this.excludedNaaccrIdCounts = excludedNaaccrIdCounts;
	}

	public Map<String, Map<String, Integer>> getExcludedOtherElementCounts()
	{
		return excludedOtherElementCounts;
	}

	public Map<String, Map<String, Integer>> getOtherElementCounts()
	{
		return otherElementCounts;
	}

	public Map<String, Integer> getExcludedElementCounts()
	{
		return excludedElementCounts;
	}

	public Map<String, Integer> getNaaccrIdCounts()
	{
		return naaccrIdCounts;
	}

	public Map<String, Map<String, Integer>> getValueCounts()
	{
		return valueCounts;
	}

	public Map<String, Integer> getExcludedNaaccrIdCounts()
	{
		return excludedNaaccrIdCounts;
	}

	public NaxMetrics()
	{
		setStartTimeMillis(System.currentTimeMillis());
	}

	public String getStartTime()
	{
		return startTime;
	}

	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}

	public String getEndTime()
	{
		return endTime;
	}

	public void setEndTime(String endTime)
	{
		this.endTime = endTime;
	}

	public Long getStartTimeMillis()
	{
		return startTimeMillis;
	}

	public void setStartTimeMillis(Long startTimeMillis)
	{
		this.startTimeMillis = startTimeMillis;

		setStartTime(DateFormatUtils.format(getStartTimeMillis(), "MM-dd-yyyy H:mm:ss:S zzz"));
	}

	public Long getEndTimeMillis()
	{
		return endTimeMillis;
	}

	public void markEndTime()
	{
		setEndTimeMillis(System.currentTimeMillis());

		setDuration(DurationFormatUtils
							.formatDuration(getEndTimeMillis() - getStartTimeMillis(), "H:mm:ss:S ('hours':'mins':'secs':'msecs')", true));
	}

	public void setEndTimeMillis(Long endTimeMillis)
	{
		this.endTimeMillis = endTimeMillis;

		setEndTime(DateFormatUtils.format(getEndTimeMillis(), "MM-dd-yyyy H:mm:ss:S zzz"));
	}

	public String getDuration()
	{
		return duration;
	}

	public void setDuration(String duration)
	{
		this.duration = duration;
	}

	public Map<String, String> getNaaccrDataAttributes()
	{
		return naaccrDataAttributes;
	}

	public void setNaaccrDataAttributes(Map<String, String> naaccrDataAttributes)
	{
		this.naaccrDataAttributes = naaccrDataAttributes;
	}
}

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
import com.imsweb.algorithms.iccc.IcccRecodeUtils;
import com.imsweb.algorithms.seersiterecode.SeerSiteRecodeUtils;
import edu.uky.kcr.nax.model.NaaccrDictionary;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration parameters for a Nax processing run, with the ability to compile Groovy scripts from a File or String
 * and load a CSV file for replacing naaccrId Item values (replacement map).
 */
public class NaxConfig
{
	@JsonIgnore
	private List<Script> compiledScripts = null;
	@JsonIgnore
	private List<Script> compiledPatientScripts = null;
	@JsonIgnore
	private List<Script> compiledTumorScripts = null;
	@JsonIgnore
	private Map<String, List<Script>> compiledItemScripts = null;
	@JsonIgnore
	private Map<String, Map<String, String>> replacementMap = null;
	@JsonIgnore
	private GroovyShell groovyShell = null;
	@JsonIgnore
	private List<NaaccrDictionary> userDictionaries = null;

	private NaxFileInfo replacementFileInfo = null;
	private List<NaxFileInfo> scriptFiles = null;
	private List<String> includedItems = null;

	@JsonIgnore
	private String emailSmtpHost = null;
	@JsonIgnore
	private List<String> emailToList = null;
	@JsonIgnore
	private String emailFrom = null;
	@JsonIgnore
	private String emailSubject = null;
	@JsonIgnore
	private String emailUsername = null;
	@JsonIgnore
	private String emailPassword = null;
	@JsonIgnore
	private Integer emailSmtpPort = null;
	@JsonIgnore
	private String emailSslSmtpPort = null;
	@JsonIgnore
	private Boolean emailSslCheckServerIdentity = null;
	@JsonIgnore
	private Boolean emailStartTlsEnabled = null;
	@JsonIgnore
	private Boolean emailStartTlsRequired = null;
	@JsonIgnore
	private Boolean emailSslOnConnect = null;

	@JsonIgnore
	private Map<String, Map<String, Script>> valueCountsScripts = null;
	@JsonIgnore
	private List<String> valueCountsSimple = null;

	@JsonIgnore
	private int metricsLogging = 1;

	private int deleteOutputFiles = 0;

	//This is the one that gets printed out in the result
	private List<String> valueCounts = null;

	private boolean includeNamespaces = true;
	private boolean removeEmptyPatients = false;
	private List<String> excludedItems = null;
	private Map<String, String> constantValueMap = null;
	private List<NaxFileInfo> userDictionaryFiles = null;

	public NaxConfig()
	{
		initialize();
	}

	private void initialize()
	{
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addStaticStars(StringUtils.class.getCanonicalName());
		importCustomizer.addImports(
				SeerSiteRecodeUtils.class.getCanonicalName(),
				IcccRecodeUtils.class.getCanonicalName());

		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.addCompilationCustomizers(importCustomizer);

		setGroovyShell(new GroovyShell(compilerConfiguration));
	}

	public NaxConfig withEmailSmtpHost(String emailSmtpHost)
	{
		setEmailSmtpHost(emailSmtpHost);

		return this;
	}

	public NaxConfig withEmailTo(String[] emailTo)
	{
		if (emailTo != null)
		{
			getEmailToList().addAll(Arrays.asList(emailTo));
		}

		return this;
	}

	public NaxConfig withEmailFrom(String emailFrom)
	{
		setEmailFrom(emailFrom);

		return this;
	}

	public NaxConfig withEmailSubject(String emailSubject)
	{
		setEmailSubject(emailSubject);

		return this;
	}

	public NaxConfig withEmailUsername(String emailUsername)
	{
		setEmailUsername(emailUsername);

		return this;
	}

	public NaxConfig withEmailPassword(String emailPassword)
	{
		setEmailPassword(emailPassword);

		return this;
	}

	public NaxConfig withEmailSmtpPort(int smtpPort)
	{
		setEmailSmtpPort(smtpPort);

		return this;
	}

	public NaxConfig withEmailSslSmtpPort(String emailSslSmtpPort)
	{
		setEmailSslSmtpPort(emailSslSmtpPort);

		return this;
	}

	public NaxConfig withEmailSslCheckServerIdentity(boolean emailSslCheckServerIdentity)
	{
		setEmailSslCheckServerIdentity(emailSslCheckServerIdentity);

		return this;
	}

	public NaxConfig withEmailSslOnConnect(boolean emailSslOnConnect)
	{
		setEmailSslOnConnect(emailSslOnConnect);
		return this;
	}

	public NaxConfig withEmailStartTlsEnabled(boolean emailStartTlsEnabled)
	{
		setEmailStartTlsEnabled(emailStartTlsEnabled);
		return this;
	}

	public NaxConfig withEmailStartTlsRequired(boolean emailStartTlsRequired)
	{
		setEmailStartTlsRequired(emailStartTlsRequired);
		return this;
	}

	public NaxConfig withConstantValue(
			String key,
			String value)
	{
		getConstantValueMap().put(key, value);

		return this;
	}

	public NaxConfig withExcludedItems(List<String> excludedItems)
	{
		getExcludedItems().addAll(excludedItems);

		return this;
	}

	public NaxConfig withIncludedItems(List<String> includedItems)
	{
		getIncludedItems().addAll(includedItems);

		return this;
	}

	public NaxConfig withValueCountsScriptString(String naaccrId, String name, String scriptString)
	{
		Map<String, Script> scriptMap = getValueCountsScripts().getOrDefault(naaccrId, new LinkedHashMap<>());

		scriptMap.put(name, compileScriptString(getGroovyShell(), scriptString));
		getValueCountsScripts().put(naaccrId, scriptMap);

		getValueCounts().add(String.format("%s/%s", naaccrId, name));

		return this;
	}

	public NaxConfig withValueCounts(String naaccrId)
	{
		getValueCountsSimple().add(naaccrId);
		getValueCounts().add(naaccrId);

		return this;
	}

	public NaxConfig withIncludeNamespaces(boolean includeNamespaces)
	{
		setIncludeNamespaces(includeNamespaces);

		return this;
	}

	public NaxConfig withRemoveEmptyPatients(boolean removeEmptyPatients)
	{
		setRemoveEmptyPatients(removeEmptyPatients);

		return this;
	}

	public NaxConfig withUserDictionary(File userDictionaryFile)
			throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException
	{
		try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(userDictionaryFile))
		{
			getUserDictionaries().add(NaaccrDictionary.createUserDictionary(inputStream));
			getUserDictionaryFiles().add(inputStream);
		}

		return this;
	}

	public NaxConfig withUserDictionary(ProgressTrackingDigestInputStream inputStream)
			throws IOException, ParserConfigurationException, SAXException
	{
		getUserDictionaries().add(NaaccrDictionary.createUserDictionary(inputStream));
		getUserDictionaryFiles().add(inputStream);

		return this;
	}

	public NaxConfig withScriptFile(File scriptFile)
			throws IOException, NoSuchAlgorithmException
	{
		try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(scriptFile))
		{
			getCompiledScripts().add(compileScriptFile(getGroovyShell(), inputStream));
			getScriptFiles().add(inputStream);
		}

		return this;
	}

	public NaxConfig withPatientScriptFile(File scriptFile)
			throws IOException, NoSuchAlgorithmException
	{
		try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(scriptFile))
		{
			getCompiledPatientScripts().add(compileScriptFile(getGroovyShell(), inputStream));
			getScriptFiles().add(inputStream);
		}

		return this;
	}

	public NaxConfig withTumorScriptFile(File scriptFile)
			throws IOException, NoSuchAlgorithmException
	{
		try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(scriptFile))
		{
			getCompiledTumorScripts().add(compileScriptFile(getGroovyShell(), inputStream));
			getScriptFiles().add(inputStream);
		}

		return this;
	}

	public NaxConfig withItemScriptFile(String naaccrId, File scriptFile)
			throws IOException, NoSuchAlgorithmException
	{
		try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(scriptFile))
		{
			List<Script> scripts = getCompiledItemScripts().getOrDefault(naaccrId, new ArrayList<>());
			scripts.add(compileScriptFile(getGroovyShell(), inputStream));
			getCompiledItemScripts().put(naaccrId, scripts);
		}

		return this;
	}

	public NaxConfig withValueCountsScriptFile(String naaccrId, String name, File scriptFile)
			throws IOException, NoSuchAlgorithmException
	{
		Map<String, Script> scriptMap = getValueCountsScripts().getOrDefault(naaccrId, new LinkedHashMap<>());

		try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(scriptFile))
		{
			scriptMap.put(name, compileScriptFile(getGroovyShell(), inputStream));
			getValueCountsScripts().put(naaccrId, scriptMap);
		}

		getValueCounts().add(String.format("%s/%s", naaccrId, name));

		return this;
	}

	public NaxConfig withScriptString(String scriptString)
	{
		getCompiledScripts().add(compileScriptString(getGroovyShell(), scriptString));

		return this;
	}

	public NaxConfig withPatientScriptString(String scriptString)
	{
		getCompiledPatientScripts().add(compileScriptString(getGroovyShell(), scriptString));

		return this;
	}

	public NaxConfig withTumorScriptString(String scriptString)
	{
		getCompiledTumorScripts().add(compileScriptString(getGroovyShell(), scriptString));

		return this;
	}

	public NaxConfig withItemScriptString(String naaccrId, String scriptString)
	{
		List<Script> scripts = getCompiledItemScripts().getOrDefault(naaccrId, new ArrayList<>());
		scripts.add(compileScriptString(getGroovyShell(), scriptString));
		getCompiledItemScripts().put(naaccrId, scripts);

		return this;
	}

	public NaxConfig withReplacementMapFile(File replacementMapFile)
			throws IOException, NoSuchAlgorithmException
	{
		setReplacementMap(loadReplacementMap(replacementMapFile));
		setReplacementFileInfo(ProgressTrackingDigestInputStream.newInstance(replacementMapFile));

		return this;
	}

	public NaxConfig withMetricsLogging(int metricsLogging)
	{
		setMetricsLogging(metricsLogging);

		return this;
	}

	public int getMetricsLogging()
	{
		return metricsLogging;
	}

	private void setMetricsLogging(int metricsLogging)
	{
		this.metricsLogging = metricsLogging;
	}

	public NaxConfig withDeleteOutputFiles(int deleteOutputFiles)
	{
		setDeleteOutputFiles(deleteOutputFiles);
		return this;
	}

	public int getDeleteOutputFiles()
	{
		return deleteOutputFiles;
	}

	private void setDeleteOutputFiles(int deleteOutputFiles)
	{
		this.deleteOutputFiles = deleteOutputFiles;
	}



	protected static Map<String, Map<String, String>> loadReplacementMap(File replacementValuesFile)
			throws IOException
	{
		Map<String, Map<String, String>> returnValue = new HashMap<>();

		try (FileReader fileReader = new FileReader(replacementValuesFile, StandardCharsets.US_ASCII))
		{
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(fileReader);

			for (CSVRecord record : records)
			{
				String naaccrId = record.get(NaxConstants.NAACCR_ID);
				String itemValue = record.get(NaxConstants.ITEM_VALUE);
				String newItemValue = record.get(NaxConstants.NEW_ITEM_VALUE);

				Map<String, String> valueMap = returnValue.getOrDefault(naaccrId, new HashMap<>());
				valueMap.put(itemValue, newItemValue);
				returnValue.put(naaccrId, valueMap);
			}
		}

		return returnValue;
	}

	protected static Script compileScriptFile(
			GroovyShell groovyShell,
			ProgressTrackingDigestInputStream scriptInputStream)
			throws IOException
	{
		String scriptString = IOUtils.toString(scriptInputStream);

		Script compiledScript = compileScriptString(groovyShell, scriptString, scriptInputStream.getName());

		return compiledScript;
	}

	protected static Script compileScriptString(
			GroovyShell groovyShell,
			String scriptString,
			String scriptName)
	{
		Script script = groovyShell.parse(scriptString);
		script.setBinding(new Binding());
		script.setProperty("name", scriptName);

		return script;
	}

	protected static Script compileScriptString(
			GroovyShell groovyShell,
			String scriptString)
	{
		return compileScriptString(groovyShell, scriptString, String.format("%s...%s", StringUtils.left(scriptString, 5), StringUtils.right(scriptString, 5)));
	}

	protected static List<Script> compileScriptStrings(
			GroovyShell groovyShell,
			List<String> scriptStrings)
	{
		List<Script> compiledScripts = new ArrayList<>();

		if ((scriptStrings != null) && (scriptStrings.size() > 0))
		{
			for (String scriptString : scriptStrings)
			{
				compiledScripts.add(compileScriptString(groovyShell, scriptString));
			}
		}

		return compiledScripts;
	}

	public GroovyShell getGroovyShell()
	{
		return groovyShell;
	}

	public void setGroovyShell(GroovyShell groovyShell)
	{
		this.groovyShell = groovyShell;
	}

	private void setCompiledScripts(List<Script> compiledScripts)
	{
		this.compiledScripts = compiledScripts;
	}

	public List<Script> getCompiledScripts()
	{
		if (this.compiledScripts == null)
		{
			this.compiledScripts = new ArrayList<>();
		}

		return compiledScripts;
	}

	private void setCompiledPatientScripts(List<Script> compiledPatientScripts)
	{
		this.compiledPatientScripts = compiledPatientScripts;
	}

	public List<Script> getCompiledPatientScripts()
	{
		if (this.compiledPatientScripts == null)
		{
			this.compiledPatientScripts = new ArrayList<>();
		}

		return compiledPatientScripts;
	}

	private void setCompiledTumorScripts(List<Script> compiledTumorScripts)
	{
		this.compiledTumorScripts = compiledTumorScripts;
	}

	public List<Script> getCompiledTumorScripts()
	{
		if (this.compiledTumorScripts == null)
		{
			this.compiledTumorScripts = new ArrayList<>();
		}

		return compiledTumorScripts;
	}

	private void setCompiledItemScripts(Map<String, List<Script>> compiledItemScripts)
	{
		this.compiledItemScripts = compiledItemScripts;
	}

	public Map<String, List<Script>> getCompiledItemScripts()
	{
		if (this.compiledItemScripts == null)
		{
			this.compiledItemScripts = new HashMap<>();
		}

		return compiledItemScripts;
	}


	public List<String> getIncludedItems()
	{
		if (this.includedItems == null)
		{
			this.includedItems = new ArrayList<>();
		}
		return includedItems;
	}

	public Map<String, Map<String, Script>> getValueCountsScripts()
	{
		if (this.valueCountsScripts == null)
		{
			this.valueCountsScripts = new HashMap<>();
		}

		return valueCountsScripts;
	}

	public List<String> getValueCountsSimple()
	{
		if (this.valueCountsSimple == null)
		{
			this.valueCountsSimple = new ArrayList<>();
		}

		return valueCountsSimple;
	}

	public List<String> getValueCounts()
	{
		if (this.valueCounts == null)
		{
			this.valueCounts = new ArrayList<>();
		}

		return valueCounts;
	}

	private void setIncludedItems(List<String> includedItems)
	{
		this.includedItems = includedItems;
	}

	public boolean isIncludeNamespaces()
	{
		return includeNamespaces;
	}

	private void setIncludeNamespaces(boolean includeNamespaces)
	{
		this.includeNamespaces = includeNamespaces;
	}

	public boolean isRemoveEmptyPatients()
	{
		return removeEmptyPatients;
	}

	public void setRemoveEmptyPatients(boolean removeEmptyPatients)
	{
		this.removeEmptyPatients = removeEmptyPatients;
	}

	public List<String> getExcludedItems()
	{
		if (this.excludedItems == null)
		{
			this.excludedItems = new ArrayList<>();
		}
		return excludedItems;
	}

	private void setExcludedItems(List<String> excludedItems)
	{
		this.excludedItems = excludedItems;
	}

	public Map<String, Map<String, String>> getReplacementMap()
	{
		if (this.replacementMap == null)
		{
			this.replacementMap = new HashMap<>();
		}
		return replacementMap;
	}

	private void setReplacementMap(Map<String, Map<String, String>> replacementMap)
	{
		this.replacementMap = replacementMap;
	}

	public Map<String, String> getConstantValueMap()
	{
		if (this.constantValueMap == null)
		{
			this.constantValueMap = new HashMap<>();
		}
		return constantValueMap;
	}

	private void setConstantValueMap(Map<String, String> constantValueMap)
	{
		this.constantValueMap = constantValueMap;
	}

	public NaxFileInfo getReplacementFileInfo()
	{
		return this.replacementFileInfo;
	}

	private void setReplacementFileInfo(NaxFileInfo naxInfoInputStream)
	{
		this.replacementFileInfo = naxInfoInputStream;
	}

	public List<NaxFileInfo> getScriptFiles()
	{
		if (this.scriptFiles == null)
		{
			this.scriptFiles = new ArrayList<>();
		}
		return scriptFiles;
	}

	private void setScriptFiles(List<NaxFileInfo> scriptFiles)
	{
		this.scriptFiles = scriptFiles;
	}

	public List<NaaccrDictionary> getUserDictionaries()
	{
		if (this.userDictionaries == null)
		{
			this.userDictionaries = new ArrayList<>();
		}
		return userDictionaries;
	}

	public void setUserDictionaries(List<NaaccrDictionary> userDictionaries)
	{
		this.userDictionaries = userDictionaries;
	}

	public List<NaxFileInfo> getUserDictionaryFiles()
	{
		if (this.userDictionaryFiles == null)
		{
			this.userDictionaryFiles = new ArrayList<>();
		}
		return userDictionaryFiles;
	}

	public void setUserDictionaryFiles(List<NaxFileInfo> userDictionaryFiles)
	{
		this.userDictionaryFiles = userDictionaryFiles;
	}

	public String getEmailSmtpHost()
	{
		return emailSmtpHost;
	}

	public void setEmailSmtpHost(String emailSmtpHost)
	{
		this.emailSmtpHost = emailSmtpHost;
	}

	public List<String> getEmailToList()
	{
		if (this.emailToList == null)
		{
			this.emailToList = new ArrayList<>();
		}

		return this.emailToList;
	}

	public String getEmailFrom()
	{
		return emailFrom;
	}

	public void setEmailFrom(String emailFrom)
	{
		this.emailFrom = emailFrom;
	}

	public String getEmailSubject()
	{
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject)
	{
		this.emailSubject = emailSubject;
	}

	public String getEmailUsername()
	{
		return emailUsername;
	}

	public void setEmailUsername(String emailUsername)
	{
		this.emailUsername = emailUsername;
	}

	public String getEmailPassword()
	{
		return emailPassword;
	}

	public void setEmailPassword(String emailPassword)
	{
		this.emailPassword = emailPassword;
	}

	public Integer getEmailSmtpPort()
	{
		return emailSmtpPort;
	}

	public void setEmailSmtpPort(Integer emailSmtpPort)
	{
		this.emailSmtpPort = emailSmtpPort;
	}

	public String getEmailSslSmtpPort()
	{
		return emailSslSmtpPort;
	}

	public void setEmailSslSmtpPort(String emailSslSmtpPort)
	{
		this.emailSslSmtpPort = emailSslSmtpPort;
	}

	public Boolean getEmailSslCheckServerIdentity()
	{
		return emailSslCheckServerIdentity;
	}

	public void setEmailSslCheckServerIdentity(Boolean emailSslCheckServerIdentity)
	{
		this.emailSslCheckServerIdentity = emailSslCheckServerIdentity;
	}

	public Boolean getEmailStartTlsEnabled()
	{
		return emailStartTlsEnabled;
	}

	public void setEmailStartTlsEnabled(Boolean emailStartTlsEnabled)
	{
		this.emailStartTlsEnabled = emailStartTlsEnabled;
	}

	public Boolean getEmailStartTlsRequired()
	{
		return emailStartTlsRequired;
	}

	public void setEmailStartTlsRequired(Boolean emailStartTlsRequired)
	{
		this.emailStartTlsRequired = emailStartTlsRequired;
	}

	public Boolean getEmailSslOnConnect()
	{
		return emailSslOnConnect;
	}

	public void setEmailSslOnConnect(Boolean emailSslOnConnect)
	{
		this.emailSslOnConnect = emailSslOnConnect;
	}
}

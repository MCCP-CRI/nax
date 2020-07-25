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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Generic JUL Log Formatter for command-line applications:
 *<ul>
 *   <li>prepends a sensible-looking timestamp to all log messages</li>
 *   <li>sends all log messages to the Console</li>
 *   <li>allows easy programmatic control of log levels</li>
 *</ul>
 */
public class ConsoleLogging
		extends Formatter
{
	private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

	public DateFormat getDateFormat()
	{
		return dateFormat;
	}

	public static void initializeConsoleLogging(Level level)
	{
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		rootLogger.setLevel(level);

		for (int i = 0; i < rootLogger.getHandlers().length; i++)
		{
			if (rootLogger.getHandlers()[i] instanceof ConsoleHandler)
			{
				rootLogger.getHandlers()[i].setFormatter(new ConsoleLogging());
				rootLogger.getHandlers()[i].setLevel(level);
			}
		}
	}

	@Override
	public String format(LogRecord record)
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append(getDateFormat().format(new Date(record.getMillis())));
		buffer.append(" ");
		buffer.append(record.getMessage());
		buffer.append('\n');

		return buffer.toString();
	}
}

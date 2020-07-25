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
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A subclass of {@link DigestInputStream} that keeps track of how many bytes have been read,
 * how many total should be read, and the name of the input.
 * Useful for showing progress to a user when reading a very large file and outputting the file hash without reading it twice.
 */
public class ProgressTrackingDigestInputStream
		extends DigestInputStream
		implements NaxFileInfo
{
	private static final String DEFAULT_MESSAGE_DIGEST_ALGORITHM = "MD5";

	@JsonIgnore
	private long totalRead = -1;
	private long totalLength = -1;
	private String name = null;
	private String md5 = null;

	@JsonIgnore
	private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	@JsonIgnore
	private static int MAX_READ_BUFFER = 1024;

	@JsonIgnore
	private String lineSeparator = null;


	public static ProgressTrackingDigestInputStream newInstance(File file)
			throws FileNotFoundException, NoSuchAlgorithmException
	{
		return new ProgressTrackingDigestInputStream(new FileInputStream(file), file.getName(), file.length());
	}

	public ProgressTrackingDigestInputStream(InputStream inputStream,
											 String name,
											 long totalLength)
			throws NoSuchAlgorithmException
	{
		super(inputStream, MessageDigest.getInstance(DEFAULT_MESSAGE_DIGEST_ALGORITHM));

		setTotalRead(0);
		setName(name);
		setTotalLength(totalLength);
	}

	@Override
	@JsonIgnore
	public MessageDigest getMessageDigest()
	{
		return super.getMessageDigest();
	}

	public String getMd5()
	{
		if (this.md5 == null)
		{
			this.md5 = Hex.encodeHexString(getMessageDigest().digest());
		}

		return this.md5;
	}

	private String determineLineSeparator(byte[] readBuffer)
	{
		String lineSeparator = null;

		String text = new String(getByteArrayOutputStream().toByteArray());
		int firstIndex = text.indexOf('\n');

		if (firstIndex > 0)
		{
			if (text.charAt(firstIndex - 1) == '\r')
			{
				lineSeparator = "\r\n";
			}
			else
			{
				lineSeparator = "\n";
			}
		}

		return lineSeparator;
	}

	@JsonIgnore
	public String getLineSeparator()
	{
		if (this.lineSeparator == null)
		{
			if (getByteArrayOutputStream().size() > 0)
			{
				this.lineSeparator = determineLineSeparator(getByteArrayOutputStream().toByteArray());
			}
		}

		return this.lineSeparator;
	}

	public long getTotalRead()
	{
		return totalRead;
	}

	private void setTotalRead(long totalRead)
	{
		this.totalRead = totalRead;
	}

	public long getTotalLength()
	{
		return totalLength;
	}

	public void setTotalLength(long totalLength)
	{
		this.totalLength = totalLength;
	}

	public String getName()
	{
		return name;

	}

	@JsonIgnore
	public ByteArrayOutputStream getByteArrayOutputStream()
	{
		return byteArrayOutputStream;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public int read()
			throws IOException
	{
		setTotalRead(getTotalRead() + 1);

		int nextRead = super.read();

		if (nextRead > 0)
		{
			if (getByteArrayOutputStream().size() < MAX_READ_BUFFER)
			{
				getByteArrayOutputStream().write(nextRead);
			}
		}

		return nextRead;
	}

	@Override
	public int read(byte[] bytes)
			throws IOException
	{
		int nextRead = super.read(bytes);

		if (nextRead > 0)
		{
			setTotalRead(getTotalRead() + nextRead);

			if (getByteArrayOutputStream().size() < MAX_READ_BUFFER)
			{
				getByteArrayOutputStream().write(bytes);
			}
		}

		return nextRead;
	}

	@Override
	public int read(byte[] bytes,
					int off,
					int len)
			throws IOException
	{
		int nextRead = super.read(bytes, off, len);

		if (nextRead > 0)
		{
			setTotalRead(getTotalRead() + nextRead);

			if (getByteArrayOutputStream().size() < MAX_READ_BUFFER)
			{
				getByteArrayOutputStream().write(bytes, off, nextRead);
			}
		}

		return nextRead;
	}
}

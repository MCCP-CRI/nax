/*
	Create a new <Item> from every <Item> where the string, 'date', is in the naaccrId. The naaccrId of the new <Item> will be a concatenation of the old
	naaccrId with the string, 'Days'. The new <Item> value will be the number of days between the dateOfBirth and the existing <Item> date. The new naaccrNum
	will be the	existing <Item> naaccrNum with 9000 added to the number.
 */
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");

if (elementName.equals("Item") && 
	naaccrId.toLowerCase().indexOf("date") > -1 && 
	itemValue.length() == 8)
{
	String newNaaccrId = String.format("%sDays", naaccrId);
	Integer naaccrNum = naaccrData.getNaaccrDictionary().getNaaccrNumMap().get(naaccrId);
	Integer newNaaccrNum = Integer.valueOf(naaccrNum.intValue() + 9000);
	String dob = patient.getItemValue("dateOfBirth");
	Date dobDate = dateFormat.parse(dob);
	Date itemDate = dateFormat.parse(itemValue);

	String days = DurationFormatUtils.formatDuration(itemDate.getTime() - dobDate.getTime(), "d");

	if (tumor != null)
	{
		tumor.addItem(newNaaccrId, newNaaccrNum, days);
	}
	else
	{
		patient.addItem(newNaaccrId, newNaaccrNum, days);
	}
	
	return false;
}


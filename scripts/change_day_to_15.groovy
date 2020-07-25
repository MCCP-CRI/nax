/*
	Change the value of every <Item> where the string, 'date', is in the naaccrId and the length of the value is 8 to a date where the day is 15 and the year
	and month remain the same.
 */
if (elementName.equals("Item") &&
	naaccrId.toLowerCase().indexOf("date") > -1 && 
	itemValue.length() == 8)
{
	item.setItemValue(itemValue.substring(0,6) + "15");
}


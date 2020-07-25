/*
    Return the Translated SEER Site code name and code value
 */

String seerSite = SeerSiteRecodeUtils.calculateSiteRecode(
        SeerSiteRecodeUtils.VERSION_2010,
        tumor.getItemValue('primarySite'),
        tumor.getItemValue('histologicTypeIcdO3'));

return String.format("%s (%s)", SeerSiteRecodeUtils.getRecodeName(seerSite), seerSite);

<?php
defined( '_JEXEC' ) or die( 'Restricted access' ); 
JLoader::import('joomla.filesystem.file');
JHtml::_('behavior.framework', true);
JHtml::_('bootstrap.framework');
$document = JFactory::getDocument();

$slogan	= $this->params->get("slogan");
$slogandisable	= $this->params->get("slogandisable");
$addthis	= $this->params->get("addthis");
$footertext	= $this->params->get("footertext");
$footerdisable	= $this->params->get("footerdisable");
$googleanalytics	= $this->params->get("googleanalytics");
$analyticsdisable	= $this->params->get("analyticsdisable");
$socialbuttons	= $this->params->get("socialbuttons");
$googletranslate	= $this->params->get("googletranslate");
$jscroll	= $this->params->get("jscroll");
$slidehome	= $this->params->get("slidehome");
$slidedesc1	= $this->params->get("slidedesc1");
$url1	= $this->params->get("url1");
$slidedesc2	= $this->params->get("slidedesc2");
$url2	= $this->params->get("url2");
$slidedesc3	= $this->params->get("slidedesc3");
$url3	= $this->params->get("url3");
$slidedesc4	= $this->params->get("slidedesc4");
$url4	= $this->params->get("url4");
?>
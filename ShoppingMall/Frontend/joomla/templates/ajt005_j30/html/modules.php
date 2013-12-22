<?php

defined('_JEXEC') or die('Restricted access');

function modChrome_jaw($module, &$params, &$attribs)
{
	if (!empty ($module->content)) : ?>
	<div class="module">
        <div class="inner">
		<?php if ($module->showtitle != 0) : ?>
		<h3 class="module-title"><?php echo $module->title; ?></h3><span class="modtitle"></span>
		<?php endif; ?>
	    <div class="module-body">
	        <?php echo $module->content; ?>
        </div>
        </div>
	</div>
<?php endif;}?>



<?php
function modChrome_ajgrid($module, &$params, &$attribs) {
?>
<div class="module <?php echo $params->get( 'moduleclass_sfx' ); ?> <?php echo $attribs['grid'] ?>">
	<?php if ($module->showtitle) : ?>
    	<h3 class="module-title"><?php echo $module->title; ?></h3>
    <?php endif; ?>
    <div class="module-body">
    	<?php echo $module->content; ?>
    </div>
</div>
<?php }?>
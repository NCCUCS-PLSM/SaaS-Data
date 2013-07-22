<?php
/**
 * @version		$Id: default.php 21322 2011-05-11 01:10:29Z dextercowley $
 * @package		Joomla.Site
 * @subpackage	mod_login
 * @copyright	Copyright (C) 2005 - 2011 Open Source Matters, Inc. All rights reserved.
 * @license		GNU General Public License version 2 or later; see LICENSE.txt
 */

// no direct access
defined('_JEXEC') or die;
JHtml::_('behavior.keepalive');
?>
<?php if ($type == 'logout') : ?>
<form action="<?php echo JRoute::_('index.php', true, $params->get('usesecure')); ?>" method="post" id="login-form">
<?php if ($params->get('greeting')) : ?>
	<div class="login-greeting">
	<?php if($params->get('name') == 0) : {
		echo JText::sprintf('MOD_LOGIN_HINAME', $user->get('name'));
	} else : {
		echo JText::sprintf('MOD_LOGIN_HINAME', $user->get('username'));
	} endif; ?>
	</div>
<?php endif; ?>
	<div class="logout-button">
		<input type="submit" name="Submit" class="btn btn-primary" value="<?php echo JText::_('JLOGOUT'); ?>" />
		<input type="hidden" name="option" value="com_users" />
		<input type="hidden" name="task" value="user.logout" />
		<input type="hidden" name="return" value="<?php echo $return; ?>" />
		<?php echo JHtml::_('form.token'); ?>
	</div>
</form>
<?php else : ?>
<form action="<?php echo JRoute::_('index.php', true, $params->get('usesecure')); ?>" method="post" id="login-form" >
	<?php if ($params->get('pretext')): ?>
		<div class="pretext">
		<p><?php echo $params->get('pretext'); ?></p>
		</div>
	<?php endif; ?>
	<fieldset class="userdata">
            <div class="control-group">
              <label class="control-label" for="inputEmail"><?php echo JText::_('MOD_LOGIN_VALUE_USERNAME') ?></label>
              <div class="controls">
                <div class="input-prepend">
                  <span class="add-on"><i class="icon-user"></i></span> <input placeholder="<?php echo JText::_('MOD_LOGIN_VALUE_USERNAME') ?>" class="span2" id="inputEmail" type="text" name="username">
                </div>
              </div>
            </div>

            <div class="control-group">
              <label class="control-label" for="inputPassword"><?php echo JText::_('JGLOBAL_PASSWORD') ?></label>
              <div class="controls">
                <div class="input-prepend">
                  <span class="add-on"><i class="icon-lock"></i></span><input placeholder="<?php echo JText::_('JGLOBAL_PASSWORD') ?>" class="span2" id="inputPassword" type="password" name="password">
                </div>
              </div>
            </div>

			<?php if (JPluginHelper::isEnabled('system', 'remember')) : ?>   
                <label class="checkbox">
                    <input id="modlgn-remember" type="checkbox" name="remember" class="inputbox" value="yes"/><?php echo JText::_('MOD_LOGIN_REMEMBER_ME') ?>
                </label>
            <?php endif; ?>
            
            <input type="submit" name="Submit" class="btn btn-primary" value="<?php echo JText::_('JLOGIN') ?>" />
            <input type="hidden" name="option" value="com_users" />
            <input type="hidden" name="task" value="user.login" />
            <input type="hidden" name="return" value="<?php echo $return; ?>" />
            <?php echo JHtml::_('form.token'); ?>
	</fieldset>


    <div class="nav nav-list">
    	<span><a href="<?php echo JRoute::_('index.php?option=com_users&view=reset'); ?>"><i class="icon-lock"></i><?php echo JText::_('MOD_LOGIN_FORGOT_YOUR_PASSWORD'); ?></a></span>
    	<span><a href="<?php echo JRoute::_('index.php?option=com_users&view=remind'); ?>"><i class="icon-user"></i><?php echo JText::_('MOD_LOGIN_FORGOT_YOUR_USERNAME'); ?></a></span>
        
    	<?php $usersConfig = JComponentHelper::getParams('com_users');	if ($usersConfig->get('allowUserRegistration')) : ?>
    	<span><a href="<?php echo JRoute::_('index.php?option=com_users&view=registration'); ?>"><i class="icon-check"></i><?php echo JText::_('MOD_LOGIN_REGISTER'); ?></a></span>
    	<?php endif; ?>
    </div>
    <div class="clr"></div>
	<?php if ($params->get('posttext')): ?>
		<div class="well well-small">
			<?php echo $params->get('posttext'); ?>
		</div>
	<?php endif; ?>
</form>
<?php endif; ?>

<?php
/**
 * @module		com_ola
 * @script		ola.php
 * @author-name Christophe Demko
 * @adapted by  Ribamar FS
 * @copyright	Copyright (C) 2012 Christophe Demko
 * @license		GNU/GPL, see http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

// No direct access to this file
defined('_JEXEC') or die;
 
/**
 * Ola component helper.
 */
abstract class OlaHelper
{
	/**
	 * Configure the Linkbar.
	 */
	public static function addSubmenu($submenu) 
	{

		JSubMenuHelper::addEntry(JText::_('COM_OLA_SUBMENU_ORDERS'), 'index.php?option=com_ola', $submenu == 'orders');
		JSubMenuHelper::addEntry(JText::_('COM_OLA_SUBMENU_PRODUCTS'), 'index.php?option=com_ola&view=products', $submenu == 'products');
		JSubMenuHelper::addEntry(JText::_('COM_OLA_SUBMENU_CUSTOMOBJECTS'), 'index.php?option=com_ola&view=customobjects', $submenu == 'customobjects');
		
		// set some global property
		$document = JFactory::getDocument();
		$document->addStyleDeclaration('.icon-48-ola {background-image: url(../media/com_ola/images/tux-48x48.png);}');
		if ($submenu == 'products') {
			$document->setTitle(JText::_('COM_OLA_SUBMENU_PRODUCTS'));
		}else if($submenu == 'customobjects'){
			$document->setTitle(JText::_('COM_OLA_SUBMENU_CUSTOMOBJECTS'));
		}
	}
	/**
	 * Get the actions
	 */
	public static function getActions($messageId = 0)
	{
		$user	= JFactory::getUser();
		$result	= new JObject;
 
		if (empty($messageId)) {
			$assetName = 'com_ola';
		}
		else {
			$assetName = 'com_ola.message.'.(int) $messageId;
		}
 
		$actions = array(
			'core.admin', 'core.manage', 'core.create', 'core.edit', 'core.delete'
		);
 
		foreach ($actions as $action) {
			$result->set($action,	$user->authorise($action, $assetName));
		}
 
		return $result;
	}
}

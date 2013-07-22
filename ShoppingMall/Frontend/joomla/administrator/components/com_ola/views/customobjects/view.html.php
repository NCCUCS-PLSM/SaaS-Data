<?php
/**
 * @module		com_ola
 * @author-name Christophe Demko
 * @adapted by  Ribamar FS
 * @copyright	Copyright (C) 2012 Christophe Demko
 * @license		GNU/GPL, see http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

// No direct access to this file
defined('_JEXEC') or die('Restricted access');
 
// import Joomla view library
jimport('joomla.application.component.view');
 
/**
 * Ola View
 */
class OlaViewCustomobjects extends JViewLegacy
{
	/**
	 * Ola view display method
	 * @return void
	 */
	function display($tpl = null) 
	{
		// Get data from the model
		//$items = $this->get('Items');
		//$pagination = $this->get('Pagination');
 
		// Check for errors.
		//if (count($errors = $this->get('Errors'))) 
		//{
		//	JError::raiseError(500, implode('<br />', $errors));
		//	return false;
		//}
		// Assign data to the view
		//$this->items = $items;
		//$this->pagination = $pagination;
 
		// Set the toolbar
		//$this->addToolBar();
 
		// Display the template
		parent::display($tpl);
 
		// Set the document
		$this->setDocument();
	}
 
	/**
	 * Setting the toolbar
	 */
	protected function addToolBar() 
	{
		$canDo = OlaHelper::getActions();
		JToolBarHelper::title(JText::_('COM_OLA_MANAGER_OLAS'), 'ola');
		if ($canDo->get('core.create')) 
		{
			JToolBarHelper::addNew('ola.add', 'JTOOLBAR_NEW');
		}
		if ($canDo->get('core.edit')) 
		{
			JToolBarHelper::editList('ola.edit', 'JTOOLBAR_EDIT');
		}
		if ($canDo->get('core.delete')) 
		{
			JToolBarHelper::deleteList('', 'ola.delete', 'JTOOLBAR_DELETE');
		}
		if ($canDo->get('core.admin')) 
		{
			JToolBarHelper::divider();
			JToolBarHelper::preferences('com_ola');
		}
	}
	/**
	 * Method to set up the document properties
	 *
	 * @return void
	 */
	protected function setDocument() 
	{
		$document = JFactory::getDocument();
		$document->setTitle(JText::_('COM_OLA_ADMINISTRATION'));
	}
}

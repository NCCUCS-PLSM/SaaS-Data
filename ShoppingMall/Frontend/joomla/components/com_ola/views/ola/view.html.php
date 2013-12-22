<?php
// No direct access to this file
defined('_JEXEC') or die('Restricted access');
 
// import Joomla view library
jimport('joomla.application.component.view');
 
/**
 * Ola class for the Ola Component
 */
class OlaViewOla extends JViewLegacy
{
	// Overwriting JView display method
	function display($tpl = null) 
	{
		// Assign data to the view
		//$this->item = $this->get('Item');
		
		//todo arthur tenantid and pid
		
		$menu = JSite::getMenu();
		$alias = $menu->getActive()->alias;
		
		//todo arthur
		$tid=0;
		if($alias == 'moviestore'){
			$tid =1;
		}else if($alias == 'pixeldot'){
			$tid =2;
		}else if($alias == 'driverally'){
			$tid =3;
		}
		
		$this->item = $tid;
		$this->pid = 1;
 
		// Check for errors.
		if (count($errors = $this->get('Errors'))) 
		{
			JError::raiseError(500, implode('<br />', $errors));
			return false;
		}
		// Display the view
		parent::display($tpl);
	}
}

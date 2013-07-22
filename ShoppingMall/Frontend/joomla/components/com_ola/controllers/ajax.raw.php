<?php

    defined('_JEXEC') or die( 'Restricted access' );
    jimport('joomla.application.component.controller');
    
    
    class OlaControllerAjax extends JControllerLegacy
    {
        function addCart()
        {
        	$user =& JFactory::getUser();
        	
        	$tid = JRequest::getVar('tid');
        	$tname = JRequest::getVar('tname');
        	$pid = JRequest::getVar('pid');
        	$pname = JRequest::getVar('pname');
        	$qty = JRequest::getVar('qty');
        	
        	$session = JFactory::getSession();
        	$uniqueName = $user->id."cart";
        	$cart = $session->get('cart', array() ,$uniqueName);
        	if($cart == null){
        		$cart = array();
        		$cart['items'][] = array('tid' => $tid, 'tname' => $tname ,'pname'=>$pname ,'pid'=>$pid,'qty'=>$qty);
        		$session->set('cart', $cart ,$uniqueName);
        	
        	}else{	
        		$cart['items'][] = array('tid' => $tid, 'tname' => $tname ,'pname'=>$pname ,'pid'=>$pid,'qty'=>$qty);
        		$session->set('cart', $cart ,$uniqueName);
        		
        	}

        }
        
        function clearSession(){
        	
        	$user =& JFactory::getUser();
        	$uniqueName = $user->id."cart";
        	$session = JFactory::getSession();
        	$session->clear('cart',$uniqueName);
        	
        }
        
    }
    
    
    
?>
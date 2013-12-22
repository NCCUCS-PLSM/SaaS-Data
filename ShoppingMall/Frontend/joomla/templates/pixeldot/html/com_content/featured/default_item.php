<?php

/**

 * @version		$Id: default_item.php 20196 2011-01-09 02:40:25Z ian $

 * @package		Joomla.Site

 * @subpackage	com_content

 * @copyright	Copyright (C) 2005 - 2011 Open Source Matters, Inc. All rights reserved.

 * @license		GNU General Public License version 2 or later; see LICENSE.txt

 */



// no direct access

defined('_JEXEC') or die;



// Create a shortcut for params.

$params = &$this->item->params;

$canEdit	= $this->item->params->get('access-edit');

?>



<?php if ($this->item->state == 0) : ?>

<div class="system-unpublished">

<?php endif; ?>

<?php if ($params->get('show_title')) : ?>

	<h2 class="item-page-title<?php echo $this->pageclass_sfx?>">

		<?php if ($params->get('link_titles') && $params->get('access-view')) : ?>

			<a href="<?php echo JRoute::_(ContentHelperRoute::getArticleRoute($this->item->slug, $this->item->catid)); ?>">

			<?php echo $this->escape($this->item->title); ?></a>

		<?php else : ?>

			<?php echo $this->escape($this->item->title); ?>

		<?php endif; ?>

	</h2>

<?php endif; ?>



<?php if ($canEdit ||  $params->get('show_print_icon') || $params->get('show_email_icon')) : ?>

		<div class="buttonheading">

		<?php if (!$this->print) : ?>

				<?php if ($params->get('show_print_icon')) : ?>

				<div class="print">

						<?php echo JHtml::_('icon.print_popup',  $this->item, $params); ?>

				</div>

				<?php endif; ?>



				<?php if ($params->get('show_email_icon')) : ?>

				<div class="email">

						<?php echo JHtml::_('icon.email',  $this->item, $params); ?>

				</div>

				<?php endif; ?>

				<?php if ($canEdit) : ?>

						<div class="edit">

							<?php echo JHtml::_('icon.edit', $this->item, $params); ?>

						</div>

					<?php endif; ?>

		<?php else : ?>

				<div>

						<?php echo JHtml::_('icon.print_screen',  $this->item, $params); ?>

				</div>

		<?php endif; ?>

		</div>

<?php endif; ?>



<?php if (!$params->get('show_intro')) : ?>

	<?php echo $this->item->event->afterDisplayTitle; ?>

<?php endif; ?>



<?php echo $this->item->event->beforeDisplayContent; ?>



<?php // to do not that elegant would be nice to group the params ?>



<?php $useDefList = (($params->get('show_author')) OR ($params->get('show_category')) OR ($params->get('show_parent_category'))

	OR ($params->get('show_create_date')) OR ($params->get('show_modify_date')) OR ($params->get('show_publish_date'))); ?>



<?php if ($useDefList) : ?>

 <div class="iteminfo">

<?php endif; ?>



<?php if ($params->get('show_parent_category') && $this->item->parent_slug != '1:root') : ?>

		<span class="category">

			<?php	$title = $this->escape($this->item->parent_title);

					$url = '<a href="'.JRoute::_(ContentHelperRoute::getCategoryRoute($this->item->parent_slug)).'">'.$title.'</a>';?>

			<?php if ($params->get('link_parent_category') AND $this->item->parent_slug) : ?>

				<?php echo JText::sprintf('COM_CONTENT_PARENT', $url); ?>

				<?php else : ?>

				<?php echo JText::sprintf('COM_CONTENT_PARENT', $title); ?>

			<?php endif; ?>

		</span>

<?php endif; ?>



<?php if ($params->get('show_category')) : ?>

		<span class="sub-category">

			<?php 	$title = $this->escape($this->item->category_title);

					$url = '<a href="'.JRoute::_(ContentHelperRoute::getCategoryRoute($this->item->catslug)).'">'.$title.'</a>';?>

			<?php if ($params->get('link_category') AND $this->item->catslug) : ?>

				<?php echo JText::sprintf('COM_CONTENT_CATEGORY', $url); ?>

				<?php else : ?>

				<?php echo JText::sprintf('COM_CONTENT_CATEGORY', $title); ?>

			<?php endif; ?>

		</span>

<?php endif; ?>

<?php if ($useDefList) : ?>

<div class="clr"></div>

<?php endif; ?>

<?php if ($params->get('show_create_date')) : ?>

		<span class="create">

		<?php echo JText::sprintf('COM_CONTENT_CREATED_DATE_ON', JHTML::_('date',$this->item->created, JText::_('DATE_FORMAT_LC1'))); ?>

		</span>

<?php endif; ?>

<?php if ($params->get('show_modify_date')) : ?>

		<span class="modified">

		<?php echo JText::sprintf('COM_CONTENT_LAST_UPDATED', JHTML::_('date',$this->item->modified, JText::_('DATE_FORMAT_LC1'))); ?>

		</span>

<?php endif; ?>

<?php if ($params->get('show_publish_date')) : ?>

		<span class="published">

		<?php echo JText::sprintf('COM_CONTENT_PUBLISHED_DATE', JHTML::_('date',$this->item->publish_up, JText::_('DATE_FORMAT_LC1'))); ?>

		</span>

<?php endif; ?>

<?php if ($params->get('show_author') && !empty($this->item->author )) : ?>

	<span class="createdby"> 

		<?php $author =  $this->item->author; ?>

		<?php $author = ($this->item->created_by_alias ? $this->item->created_by_alias : $author);?>



			<?php if (!empty($this->item->contactid ) &&  $params->get('link_author') == true):?>

				<?php 	echo JText::sprintf('COM_CONTENT_WRITTEN_BY' , 

				 JHTML::_('link',JRoute::_('index.php?option=com_contact&view=contact&id='.$this->item->contactid),$author)); ?>



			<?php else :?>

				<?php echo JText::sprintf('COM_CONTENT_WRITTEN_BY', $author); ?>

			<?php endif; ?>

	</span>

<?php endif; ?>	

<?php if ($useDefList) : ?>

 </div>

<?php endif; ?>



<?php echo $this->item->introtext; ?>



<?php if ($params->get('show_readmore') && $this->item->readmore) :

	if ($params->get('access-view')) :

		$link = JRoute::_(ContentHelperRoute::getArticleRoute($this->item->slug, $this->item->catid));

	else :

		$menu = JFactory::getApplication()->getMenu();

		$active = $menu->getActive();

		$itemId = $active->id;

		$link1 = JRoute::_('index.php?option=com_users&view=login&&Itemid=' . $itemId);

		$returnURL = JRoute::_(ContentHelperRoute::getArticleRoute($this->item->slug, $this->item->catid));

		$link = new JURI($link1);

		$link->setVar('return', base64_encode($returnURL));

	endif;

?>

			<p class="readmore">

				<a href="<?php echo $link; ?>">

					<?php if (!$params->get('access-view')) :

						echo JText::_('COM_CONTENT_REGISTER_TO_READ_MORE');

					elseif ($readmore = $this->item->alternative_readmore) :

						echo $readmore;

						echo JHTML::_('string.truncate', ($this->item->title), $params->get('readmore_limit'));

					elseif ($params->get('show_readmore_title', 0) == 0) :

						echo JText::sprintf('COM_CONTENT_READ_MORE_TITLE');	

					else :

						echo JText::_('COM_CONTENT_READ_MORE');

						echo JHTML::_('string.truncate', ($this->item->title), $params->get('readmore_limit'));

					endif; ?></a>

		</p>

<?php endif; ?>



<?php if ($this->item->state == 0) : ?>

</div>

<?php endif; ?>



<div class="item-separator"></div>

<?php echo $this->item->event->afterDisplayContent; ?>


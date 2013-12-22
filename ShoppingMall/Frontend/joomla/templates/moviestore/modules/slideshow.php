
				<div id="featured-slider"> 
			<img src="<?php echo $this->baseurl ?>/templates/<?php echo $this->template ?>/slideshow/1.jpg" data-caption="#Caption1" />
			<img src="<?php echo $this->baseurl ?>/templates/<?php echo $this->template ?>/slideshow/2.jpg" data-caption="#Caption2" />
			<img src="<?php echo $this->baseurl ?>/templates/<?php echo $this->template ?>/slideshow/3.jpg"  data-caption="#Caption3" />
			<img src="<?php echo $this->baseurl ?>/templates/<?php echo $this->template ?>/slideshow/4.jpg"  data-caption="#Caption4" />
		</div>
		<!-- Captions for Orbit -->
		<span class="orbit-caption" id="Caption1"><a href="<?php if ($this->params->get( 'url1' )) : ?><?php echo ($url1); ?><?php endif; ?>">
		<?php if ($this->params->get( 'slidedesc1' )) : ?><?php echo ($slidedesc1); ?><?php endif; ?></span></a>
		
		<span class="orbit-caption" id="Caption2"><a href="<?php if ($this->params->get( 'url2' )) : ?><?php echo ($url2); ?><?php endif; ?>">
		<?php if ($this->params->get( 'slidedesc2' )) : ?><?php echo ($slidedesc2); ?><?php endif; ?></span></a>
		
		<span class="orbit-caption" id="Caption3"><a href="<?php if ($this->params->get( 'url3' )) : ?><?php echo ($url3); ?><?php endif; ?>">
		<?php if ($this->params->get( 'slidedesc3' )) : ?><?php echo ($slidedesc3); ?><?php endif; ?></span></a>
		
		<span class="orbit-caption" id="Caption4"><a href="<?php if ($this->params->get( 'url4' )) : ?><?php echo ($url4); ?><?php endif; ?>">
		<?php if ($this->params->get( 'slidedesc4' )) : ?><?php echo ($slidedesc4); ?><?php endif; ?></span></a>

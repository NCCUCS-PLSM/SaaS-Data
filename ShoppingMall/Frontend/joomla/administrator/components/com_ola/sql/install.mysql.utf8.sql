DROP TABLE IF EXISTS `#__ola`;
 
CREATE TABLE `#__ola` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bemvindo` varchar(25) NOT NULL,
  `catid` int(11) NOT NULL DEFAULT '0',
  `params` TEXT NOT NULL DEFAULT '',
   PRIMARY KEY  (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;
 
INSERT INTO `#__ola` (`bemvindo`) VALUES
        ('Olá!'),
        ('Adeus Mundo Cruel!');

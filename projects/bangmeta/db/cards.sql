/**
 * $Id$
 */

drop table if exists cards;

create table cards
(
     id integer not null auto_increment,
     name varchar(32) not null,
     town varchar(32) not null,
     ident varchar(32) not null,
     frequency integer not null,
     scrip integer not null,
     coins integer not null,
     effect varchar(255) not null,
     duration integer not null,
     qualifier varchar(255) not null,
     bonus_model varchar(255) not null,
     bonus_model_path varchar(255) not null,
     activation_viz varchar(255) not null,
     activation_viz_path varchar(255) not null,
     activation_sound varchar(255) not null,
     activation_sound_path varchar(255) not null,
     ongoing_viz varchar(255) not null,
     ongoing_viz_path varchar(255) not null,
     special_sound varchar(255) not null,
     special_sound_path varchar(255) not null,
     implemented tinyint default '0' not null ,
     primary key (id)
);

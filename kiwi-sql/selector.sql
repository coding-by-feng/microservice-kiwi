select *
from word_main t
where word_id = 1287623;
# where word_name like 'slap';
# where word_name = 'ISO';
# where t.word_name = 'get it together';
# where t.word_name like '% %';
# where in_time > '2020-10-09';

select wm.word_name, count(wm.word_name)
from word_main wm
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name;


select count(1)
from (select wm.word_name, count(1)
      from word_main wm
      group by wm.word_name
      having count(wm.word_name) > 1
      order by wm.word_name) temp;

select count(1)
from (select wm.word_name, count(1)
      from word_main wm
               left join word_fetch_queue wfq on wm.word_name = wfq.word_name
      where wfq.is_lock = 0
      group by wm.word_name
      having count(wm.word_name) > 1) temp;


select wm.word_name, count(1)
from word_main wm
         left join word_fetch_queue wfq on wm.word_name = wfq.word_name
where wfq.is_lock = 1
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name;

select *
from word_main_variant
where variant_name = 'tuesday';

select *
from word_paraphrase_phrase;

select distinct wm.word_name
from word_paraphrase wp
         left join word_main wm on wp.word_id = wm.word_id
where wp.meaning_chinese like '%测试%';

select *
from sys_user
# where user_id = '3612168'
order by create_time desc;

select *
from phrase_main;

select *
from word_paraphrase wp
         left join word_main wm on wp.word_id = wm.word_id
where wm.word_name = 'ingress';

select *
from word_paraphrase_star_rel wpsr
         left join word_paraphrase wp on wpsr.paraphrase_id = wp.paraphrase_id
where wp.paraphrase_id = '5406457';

select distinct character_code
from word_character;

select *
from word_paraphrase_example;

select *
from star_rel_his;

select *
from word_paraphrase_star_rel;
select *
from word_paraphrase_star_list
where owner = '1286037';

select wm.word_name, count(1)
from word_main wm
         left join word_fetch_queue wfq on wm.word_name = wfq.word_name
where wfq.is_lock = 1
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name;

select wm.word_name, count(1)
from word_main wm
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name;

select wm.word_name, wm.word_id, wc.character_id, wp.*
from word_pronunciation wp
         left join word_main wm on wp.word_id = wm.word_id
         left join word_character wc on wc.character_id = wp.character_id
where soundmark = '/ədˈvæns/';

select *
from word_pronunciation
where pronunciation_id = 2782411;
select *
from word_pronunciation;

explain
select (ifnull(wmv.variant_name, word_name)) value
from word_main wm
         left join word_main_variant wmv on wm.word_id = wmv.word_id
where wm.word_name like 'u%'
  and wm.is_del = 0
order by wm.word_name;


select wm.word_name
from word_main wm
         left join word_fetch_queue wfq on wm.word_name = wfq.word_name
where wfq.is_lock = 0
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name;

select (ifnull(wmv.variant_name, word_name)) value
from word_main wm
         left join word_main_variant wmv on wm.word_id = wmv.word_id
where wm.word_name like 'slap%'
    and wm.is_del = 0
   or wmv.variant_name like 'slap%'
order by wm.word_name;


select distinct value
from (select wm.word_name value
      from word_main wm
      where wm.word_name like 'slap%'
      union
      select wmv.variant_name value
      from word_main_variant wmv
      where wmv.variant_name like 'slap%') tmp;


select *
from star_rel_his
limit 0, 100;

select *
from word_breakpoint_review;

select *
from word_review_daily_counter;

truncate word_review_daily_counter;

select *
from t_ins_sequence;

select *
from word_review_audio;

select *
from t_ins_sequence;

select count(distinct paraphrase_id)
from word_paraphrase_star_rel wpsr
where exists(select 1 from word_paraphrase wp where wp.paraphrase_id = wpsr.paraphrase_id)
  and not exists(select 1 from word_review_audio wra where wra.source_id = wpsr.paraphrase_id)
# limit 10;

select distinct(paraphrase_id)
from word_paraphrase_star_rel wpsr
where exists(select 1 from word_paraphrase wp where wp.paraphrase_id = wpsr.paraphrase_id)
  and not exists(select 1 from word_review_audio wra where wra.source_id = wpsr.paraphrase_id)
limit 10;

select distinct(paraphrase_id)
from word_paraphrase wp
where exists(select 1 from word_main wm where wm.word_id = wp.word_id)
  and not exists(select 1 from word_review_audio wra where wra.source_id = wp.paraphrase_id)
limit 10;

select count(distinct paraphrase_id)
from word_paraphrase wp
where exists(select 1 from word_review_audio wra where wra.source_id = wp.paraphrase_id);

select count(distinct paraphrase_id)
from word_paraphrase wp
where wp.is_del = 0
  and exists(select 1 from word_main wm where wm.word_id = wp.word_id)
  and not exists(select 1 from word_review_audio wra where wra.source_id = wp.paraphrase_id)
limit 10;



select *
from word_paraphrase
where paraphrase_id = '1283254';
select *
from word_paraphrase_example
where example_id = '1283254';
select *
from word_review_audio
where source_id = '1283254';
select count(*)
from word_review_audio;

select *
from word_paraphrase
where paraphrase_id = 2107009;

select *
from word_main wm
where wm.info_type = 1
limit 1;

delete
from word_review_audio
where source_id
          in (select wra.source_id
              from (select distinct source_id
                    from word_review_audio
                    group by source_id, type
                    having count(1) > 1) wra);

select *
from word_review_audio
where source_id = '1283430'
  and type = 1;

select *
from word_review_daily_counter
order by today desc;

delete
from word_review_daily_counter
where user_id = 1
  and today = '2022-08-28';


select client_id,
       client_secret,
       resource_ids,
       scope,
       authorized_grant_types,
       web_server_redirect_uri,
       authorities,
       access_token_validity,
       refresh_token_validity,
       additional_information,
       autoapprove
from sys_oauth_client_details
where client_id = 'test';

select *
from sys_oauth_client_details;

select *
from sys_user;

select count(distinct (paraphrase_id))
from word_paraphrase_star_rel wpsr
where not exists(select 1
                 from word_review_audio_generation wrag
                 where wrag.source_id = wpsr.paraphrase_id
                   and wrag.type = 5
                   and wrag.is_finish = 1)
limit 10;

select source_text
from word_review_audio
where type = 7
group by source_text;

select *
from word_paraphrase;

truncate word_review_audio_generation;

delete
from word_review_audio_generation
where type = 5;


select *
from word_review_audio_generation;

select paraphrase_id
from word_paraphrase_star_rel wpsr
where not exists(select 1
                 from word_review_audio_generation wrag
                 where wrag.source_id = wpsr.paraphrase_id
                   and wrag.type = 5
                   and wrag.is_finish = 1)
order by wpsr.create_time desc
limit 10;

select *
from word_paraphrase_star_rel wpsr
where wpsr.paraphrase_id = 2539690
  and list_id = 1266094;


select wp.paraphrase_id
from word_paraphrase_star_rel wpsr
         left join word_paraphrase wp on wpsr.paraphrase_id = wp.paraphrase_id
where wp.character_id = 0
  and not exists(select 1
                 from word_review_audio_generation wrag
                 where wrag.source_id = wpsr.paraphrase_id
                   and wrag.type = 10
                   and wrag.is_finish = 1)
order by wpsr.create_time desc
limit 10;

select wc.character_code, wp.paraphrase_id, wp.word_id
from word_paraphrase wp
         left join word_character wc on wp.character_id = wc.character_id
where wp.is_del = 0
  and wc.character_code = 'adverb preposition';

select *
from word_main
where word_name = 'aboard';

select distinct character_code
from word_character;

select *
from word_paraphrase_star_rel
order by create_time desc;

select wp.paraphrase_id, wp.word_id
from word_paraphrase_star_rel wpsr
         left join word_paraphrase wp on wpsr.paraphrase_id = wp.paraphrase_id
where wp.character_id = 0
  and not exists(select 1
                 from word_review_audio_generation wrag
                 where wrag.source_id = wp.word_id
                   and wrag.type = 10
                   and wrag.is_finish = 1)
limit 10;

select * from t_ins_sequence;

insert into t_ins_sequence value (5000000, 'b');


select *
from word_review_audio wra
order by id desc;
# where id = 1282838;
# where wra.source_url = 'voicerss'
# and wra.type = 3
# and wra.is_del = 0
# limit 1000;

select wpsr.paraphrase_id
from word_paraphrase_star_rel wpsr
         left join word_paraphrase wp
                   on wpsr.paraphrase_id = wp.paraphrase_id
where not exists(select 1
                 from word_review_audio_generation wrag
                 where wrag.source_id = wpsr.paraphrase_id
                   and wrag.type = 5
                   and wrag.is_finish = 1)
  and wp.is_del = 0
order by wpsr.create_time desc
limit 10;

select wpsr.create_time, wpsr.paraphrase_id, wpsl.*
from word_paraphrase_star_rel wpsr
         left join word_paraphrase_star_list wpsl
                   on wpsl.id = wpsr.list_id
order by wpsr.create_time desc
limit 100;
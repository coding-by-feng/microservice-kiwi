select *
from word_character wc
where not exists(select 1 from word_main wm where wm.word_id = wc.word_id);

delete
from word_character
where word_id in (select tmp.word_id
                  from (select wpe.word_id
                        from word_character wpe
                        where not exists(
                                select *
                                from word_main wp
                                where wp.word_id = wpe.word_id)) tmp);


select wmv.*
from word_main_variant wmv
where not exists(select 1 from word_main wm where wm.word_id = wmv.word_id);

delete
from word_main_variant
where id in (select tmp.id
             from (select wmv.id
                   from word_main_variant wmv
                   where not exists(select 1 from word_main wm where wm.word_id = wmv.word_id)) tmp);

select count(1)
from word_paraphrase_example;

select *
from word_paraphrase_example
where not exists(select wp.paraphrase_id from word_paraphrase wp where wp.paraphrase_id = paraphrase_id);

delete
from word_paraphrase_example
where paraphrase_id in (select tmp.paraphrase_id
                        from (select wpe.paraphrase_id
                              from word_paraphrase_example wpe
                              where not exists(
                                      select *
                                      from word_paraphrase wp
                                      where wp.paraphrase_id = wpe.paraphrase_id)) tmp);

update word_fetch_queue
set word_id=0
where queue_id in (select tmp.queue_id
                   from (select wfq.queue_id
                         from word_fetch_queue wfq
                         where wfq.word_id <> 0
                           and not exists(select * from word_main wm where wm.word_id = wfq.word_id)) tmp);

select *
from word_paraphrase wp
where not exists(select 1 from word_main wm where wm.word_id = wp.word_id);

delete
from word_paraphrase
where word_id in (select tmp.word_id
                  from (select wpe.word_id
                        from word_paraphrase wpe
                        where not exists(
                                select *
                                from word_main wp
                                where wp.word_id = wpe.word_id)) tmp);

delete
from word_pronunciation
where word_id in (select tmp.word_id
                  from (select wp.word_id
                        from word_pronunciation wp
                        where not exists(
                                select *
                                from word_main wm
                                where wm.word_id = wp.word_id)) tmp);


delete
from word_main
where word_id not in (select temp.word_id
                      from (select max(wm.word_id) word_id
                            from word_main wm
                            group by wm.word_name
                            having count(wm.word_name) > 1) temp);

select count(1)
from word_star_rel;

truncate word_paraphrase_star_rel;
truncate word_example_star_rel;
truncate word_star_rel;

select wm.word_name, count(1)
from word_main wm
         left join word_fetch_queue wfq on wm.word_name = wfq.word_name
where wfq.is_lock = 0
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name;

truncate star_rel_his;


select *
# delete
from word_main
where in_time > '2020-10-09';

delete
from word_paraphrase_star_rel
where paraphrase_id in (select tmp.paraphrase_id
                        from (select wpsr.paraphrase_id
                              from word_paraphrase_star_rel wpsr
                              where not exists(
                                      select *
                                      from word_paraphrase wp
                                      where wp.paraphrase_id = wpsr.paraphrase_id
                                        and is_del = 0)) tmp);
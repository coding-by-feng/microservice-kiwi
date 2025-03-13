update word_fetch_queue
set
     fetch_status= 0,
     fetch_time=0,
     fetch_result='',
     is_lock=1,
    is_into_cache = 0
#  where fetch_status = 666;
    where is_valid = 'N';
# where word_name = 'abandon';
# where fetch_time > 100;
#   and fetch_status <> 666
#   and in_time > '2020-10-14';
# where in_time > '2020-10-09';

update word_fetch_queue
set word_id=(select wm.word_id from word_main wm where wm.word_name = word_name limit 1)
where fetch_status = 666
  and word_id = 0;

select word_name, count(word_name)
from word_main
group by word_name
having count(word_name) > 1;

#   and fetch_status < 0
#   and fetch_status <> -5;
# where fetch_status < 0;

select word_name, count(word_name)
from word_fetch_queue
# where in_time > '2020-10-09'
group by word_name
having count(word_name) > 1;


# and info_type = 2;
# where fetch_status = 200;

select count(1)
# select *
from word_fetch_queue
# where fetch_status = 666
#   and is_into_cache = 0;
# where fetch_status = 80;
# where fetch_status = 3;
#   and word_id <> 0;
# where derivation IS NULL;
# where info_type = 2;
where fetch_status < 0;

select fetch_status, count(fetch_status)
from word_fetch_queue
# where in_time > '2020-10-09'
group by fetch_status;

select fetch_time, count(fetch_time)
from word_fetch_queue
# where in_time > '2020-10-09'
group by fetch_time;

select *
from word_fetch_queue t
where t.word_name like 'subsidiary';
# where word_name like '% %';
# where derivation IS NULL;
# where derivation = 'adds';
# where fetch_result like 'word% has a variant%'

update word_fetch_queue
set fetch_status=0,
#     is_valid='Y',
    is_lock=1,
    fetch_time=0,
    fetch_result=''
#     is_into_cache=0
# where word_name in (select * from queue_tmp);
where word_name like 'rush hours';
# where word_name like '% %';
# where fetch_status = -3;

select *
from word_fetch_queue
where word_name in (select * from queue_tmp);

create temporary table queue_tmp(select wm.word_name
                                 from word_main wm
                                 group by wm.word_name
                                 having count(wm.word_name) > 1);

CREATE TEMPORARY TABLE queue_tmp (select (select word_name from word_main where word_id = swp.word_id) word_name
                                  from word_paraphrase swp
                                  where swp.paraphrase_english in (select wp.paraphrase_english
                                                                   from word_paraphrase_star_rel wpsr
                                                                            left join word_paraphrase wp
                                                                                      on wpsr.paraphrase_id = wp.paraphrase_id
                                                                   where not exists(select 1 from word_main wm where wm.word_id = wp.word_id))
                                    and exists(select 1 from word_main swm where swm.word_id = swp.word_id));

select fetch_time, fetch_status, wfq.*
from word_fetch_queue wfq
order by wfq.in_time desc;

select distinct fetch_status
from word_fetch_queue;

delete
from word_fetch_queue
where word_name = 'nominal';


select *
from word_fetch_queue
where word_name like 'accede to sth';

select count(wfq.fetch_status), wfq.fetch_status
from word_fetch_queue wfq
group by wfq.fetch_status;

select *
from finance_pay_record_internal;

select *
from word_pronunciation;

# source_url、file_path、group_name

select wfq.*
from word_fetch_queue wfq
order by wfq.in_time desc;


select wm.word_name
from word_main wm
         left join word_fetch_queue wfq on wm.word_name = wfq.word_name
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name
limit 10;

update word_fetch_queue set is_valid='N';

select count(1)
# select *
from word_fetch_queue where fetch_status = 666;
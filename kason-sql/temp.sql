select wm.word_name
from word_main wm
         left join word_fetch_queue wfq on wm.word_name = wfq.word_name
# where wfq.is_lock = 0
group by wm.word_name
having count(wm.word_name) > 1
order by wm.word_name
limit 5;

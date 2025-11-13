select *
from word_star_list;

create procedure initStarList(username int)
begin
    declare userID int default 0;
    declare word_list_cursor cursor for select * from word_star_list where owner = 1;
    declare paraphrase_list_cursor cursor for select * from word_paraphrase_star_list where owner = 1;
    declare example_list_cursor cursor for select * from word_example_star_list where owner = 1;
    select su.user_id into userID from sys_user su where su.username = username;

end;

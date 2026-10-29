package ru.moyritm.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val bg = Color.rgb(17,21,29)
    private val panel = Color.rgb(27,34,48)
    private val panel2 = Color.rgb(37,48,66)
    private val line = Color.rgb(48,59,77)
    private val white = Color.rgb(242,245,250)
    private val muted = Color.rgb(155,169,189)
    private val blue = Color.rgb(131,181,255)
    private val green = Color.rgb(88,197,165)
    private val yellow = Color.rgb(241,184,91)
    private val purple = Color.rgb(179,154,247)
    private val prefs by lazy { getSharedPreferences("moy_ritm_data", MODE_PRIVATE) }
    private var page = "home"
    private var selectedDate = dateString(Date())
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout

    data class Task(val id: String, val title: String, val date: String, val priority: String, var done: Boolean)
    data class Workout(val date: String, val name: String, val details: String)
    data class Sleep(val date: String, val bed: String, val wake: String, val hours: Double, val quality: Int)

    private val subjects = listOf(
        "Программирование для анализа данных", "Иностранный язык в профессиональной деятельности",
        "Проектный практикум", "Статистические пакеты прикладных программ",
        "Математическая экономика", "Логистика", "Программная инженерия"
    )
    private data class Event(val start: String, val end: String, val title: String, val category: String, val room: String = "")
    private fun events(day: Int, date: String = selectedDate): List<Event> {
        val result = mutableListOf<Event>()
        if (day in listOf(1,2,4,5,6)) result.add(Event("08:00","09:00","Спортзал","gym"))
        when(day) {
            1 -> {
                result.add(Event("09:50","11:25",subjects[0],"study","519"))
                result.add(Event("11:40","13:15",subjects[0],"study","519"))
                result.add(Event("14:00","15:35","Логистика","study","519"))
            }
            2 -> {
                result.add(Event("09:50","11:25",subjects[1],"study","426"))
                result.add(Event("14:00","15:35",subjects[2],"study","445"))
                result.add(Event("15:50","17:25",subjects[3],"study","324"))
            }
            3 -> result.add(Event("09:00","20:00","Работа","work"))
            4 -> {
                result.add(Event("09:50","11:25",subjects[4],"study","533"))
                result.add(Event("11:40","13:15",subjects[4],"study","533"))
                result.add(Event("14:00","15:35",subjects[6],"study","533"))
            }
            5,6,0 -> result.add(Event("09:00","20:00","Работа","work"))
        }
        val custom = readArray("custom_events")
        for (i in 0 until custom.length()) {
            val o = custom.optJSONObject(i) ?: continue
            if (o.optString("date") == date) {
                result.add(Event(o.optString("start"), o.optString("end"), o.optString("title"), o.optString("category", "personal")))
            }
        }
        return result.sortedBy { it.start }
    }
    private fun dayOf(date: String): Int {
        val c = Calendar.getInstance()
        c.time = parseDate(date)
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1 // Monday=1 ... Sunday=7
    }
    private fun dayOfCalendar(date: String): Int {
        val c=Calendar.getInstance(); c.time=parseDate(date)
        return c.get(Calendar.DAY_OF_WEEK)-1
    }
    private fun parseDate(s:String): Date = try { SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(s) ?: Date() } catch(e:Exception){Date()}
    private fun dateString(d: Date): String = SimpleDateFormat("yyyy-MM-dd",Locale.US).format(d)
    private fun displayDate(s:String): String = SimpleDateFormat("EEEE, d MMMM",Locale("ru")).format(parseDate(s))
    private fun shiftDate(s:String,n:Int):String { val c=Calendar.getInstance();c.time=parseDate(s);c.add(Calendar.DAY_OF_MONTH,n);return dateString(c.time) }
    private fun readArray(key:String): JSONArray = try { JSONArray(prefs.getString(key,"[]")) } catch(e:Exception){JSONArray()}
    private fun saveArray(key:String,a:JSONArray){prefs.edit().putString(key,a.toString()).apply()}
    private fun tasks(): MutableList<Task> {
        val a=readArray("tasks"); val out=mutableListOf<Task>()
        for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;out.add(Task(o.optString("id"),o.optString("title"),o.optString("date"),o.optString("priority","normal"),o.optBoolean("done")))}
        return out
    }
    private fun saveTasks(items:List<Task>){val a=JSONArray();items.forEach{a.put(JSONObject().put("id",it.id).put("title",it.title).put("date",it.date).put("priority",it.priority).put("done",it.done))};saveArray("tasks",a)}
    private fun workouts():MutableList<Workout>{val a=readArray("workouts");val out=mutableListOf<Workout>();for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;out.add(Workout(o.optString("date"),o.optString("name"),o.optString("details")))};return out}
    private fun saveWorkouts(items:List<Workout>){val a=JSONArray();items.forEach{a.put(JSONObject().put("date",it.date).put("name",it.name).put("details",it.details))};saveArray("workouts",a)}
    private fun sleeps():MutableList<Sleep>{val a=readArray("sleep");val out=mutableListOf<Sleep>();for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;out.add(Sleep(o.optString("date"),o.optString("bed"),o.optString("wake"),o.optDouble("hours"),o.optInt("quality",3)))};return out}
    private fun saveSleeps(items:List<Sleep>){val a=JSONArray();items.forEach{a.put(JSONObject().put("date",it.date).put("bed",it.bed).put("wake",it.wake).put("hours",it.hours).put("quality",it.quality))};saveArray("sleep",a)}
    private fun habitKey(id:String,date:String)="habit_${id}_$date"
    private fun habitDone(id:String,date:String)=prefs.getBoolean(habitKey(id,date),false)
    private fun setHabit(id:String,date:String,done:Boolean){prefs.edit().putBoolean(habitKey(id,date),done).apply()}
    private val habits = listOf(
        Triple("Подъём в 7:00","07:00", listOf(0,1,2,3,4,5,6)),
        Triple("Спортзал","08:00", listOf(1,2,4,5,6)),
        Triple("Подготовка ко сну","21:00", listOf(0,1,2,3,4,5,6))
    )

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=bg;window.navigationBarColor=bg; render() }

    private fun render() {
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(10))}
        val logo=text("◒",24,blue,true);logo.gravity=Gravity.CENTER;logo.background=rounded(panel2,14);header.addView(logo,LinearLayout.LayoutParams(dp(42),dp(42)))
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,0,0)}
        brand.addView(text("Мой ритм",19,white,true));brand.addView(text("Личный планировщик",12,muted,false))
        header.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        val backup=button("⇧",panel2){backupDialog()};header.addView(backup)
        root.addView(header)
        content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(6),dp(14),dp(18))}
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(content)}
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(5),dp(6),dp(5),dp(6));setBackgroundColor(Color.rgb(20,25,34))}
        val tabs=listOf("home" to "⌂\nГлавная","plan" to "▦\nПлан","tasks" to "☑\nЗадачи","gym" to "✦\nСпорт","progress" to "▥\nПрогресс")
        tabs.forEach{(key,label)->val b=TextView(this).apply{text=label;gravity=Gravity.CENTER;textSize=11f;setTextColor(if(page==key)blue else muted);setPadding(dp(3),dp(8),dp(3),dp(8));background=if(page==key)rounded(panel2,12) else null;setOnClickListener{page=key;render()}}
            nav.addView(b,LinearLayout.LayoutParams(0,-2,1f))}
        root.addView(nav)
        setContentView(root)
        when(page){"home"->homePage();"plan"->planPage();"tasks"->tasksPage();"gym"->gymPage();else->progressPage()}
    }
    private fun addTitle(title:String,subtitle:String?=null){content.addView(text(title,25,white,true));if(subtitle!=null){val t=text(subtitle,13,muted,false);t.setPadding(0,dp(3),0,dp(13));content.addView(t)}else content.addView(space(10))}
    private fun card():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(14));background=rounded(panel,18);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)}}
    private fun addCardTitle(c:LinearLayout,s:String){c.addView(text(s,16,white,true));val v=space(8);c.addView(v)}
    private fun homePage(){
        addTitle("Мой день",displayDate(selectedDate))
        val today=dateString(Date())
        val es=events(dayOfCalendar(selectedDate), selectedDate)
        val doneTasks=tasks().count{it.date==selectedDate&&it.done};val dayTasks=tasks().count{it.date==selectedDate}
        val dueHabits=habits.filter{it.third.contains(dayOfCalendar(selectedDate))}
        val doneHabits=dueHabits.count{habitDone(it.first,selectedDate)}
        val hero=card().apply{background=rounded(Color.rgb(37,53,77),20)}
        hero.addView(text(if(selectedDate==today)"ТВОЙ РИТМ · СЕГОДНЯ" else "ВЫБРАННАЯ ДАТА",11,blue,true))
        hero.addView(text(if(selectedDate==today)SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()) else selectedDate,31,white,true))
        val next=es.firstOrNull{it.end>SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())}
        hero.addView(text(next?.let{"Следующее: ${it.title} · ${it.start}"}?:if(es.isEmpty())"Свободный день" else "Посмотри план на день",13,muted,false))
        hero.addView(space(12));hero.addView(text("Задачи $doneTasks/$dayTasks     Привычки $doneHabits/${dueHabits.size}",13,white,true))
        content.addView(hero)
        val schedule=card();addCardTitle(schedule,"Расписание")
        if(es.isEmpty())schedule.addView(text("На сегодня событий нет.",14,muted,false))
        es.forEach{schedule.addView(eventRow(it))}
        schedule.addView(button("＋ Добавить событие",panel2){eventDialog()})
        content.addView(schedule)
        val habit=card();addCardTitle(habit,"Привычки")
        dueHabits.forEach{(name,time,_)->
            val cb=CheckBox(this).apply{text="$time  $name";textSize=14f;setTextColor(white);buttonTintList=android.content.res.ColorStateList.valueOf(green);isChecked=habitDone(name,selectedDate);setOnCheckedChangeListener{_,v->setHabit(name,selectedDate,v)}}
            habit.addView(cb)
        }
        habit.addView(button("＋ Добавить привычку",panel2){habitDialog()});content.addView(habit)
        val taskCard=card();addCardTitle(taskCard,"Задачи на день")
        val todayTasks=tasks().filter{it.date==selectedDate}
        if(todayTasks.isEmpty())taskCard.addView(text("Задач пока нет.",13,muted,false))
        todayTasks.forEach{t->taskCard.addView(taskRow(t))}
        taskCard.addView(button("＋ Новая задача",blue){taskDialog()});content.addView(taskCard)
        val dateRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        dateRow.addView(button("‹ День назад",panel2){selectedDate=shiftDate(selectedDate,-1);render()},LinearLayout.LayoutParams(0,dp(44),1f))
        dateRow.addView(spaceWidth(8))
        dateRow.addView(button("Сегодня",panel2){selectedDate=today;render()},LinearLayout.LayoutParams(0,dp(44),1f))
        dateRow.addView(spaceWidth(8))
        dateRow.addView(button("День вперёд ›",panel2){selectedDate=shiftDate(selectedDate,1);render()},LinearLayout.LayoutParams(0,dp(44),1f))
        content.addView(dateRow)
    }
    private fun planPage(){
        addTitle("Расписание","Учёба, работа, спорт и личные дела")
        val dateControls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        dateControls.addView(button("‹",panel2){selectedDate=shiftDate(selectedDate,-1);render()},LinearLayout.LayoutParams(dp(48),dp(44)))
        val label=text(displayDate(selectedDate),14,white,true).apply{gravity=Gravity.CENTER}
        dateControls.addView(label,LinearLayout.LayoutParams(0,dp(44),1f))
        dateControls.addView(button("Сегодня",panel2){selectedDate=dateString(Date());render()},LinearLayout.LayoutParams(-2,dp(44)))
        content.addView(dateControls);content.addView(space(12))
        val week=card();addCardTitle(week,"Неделя")
        val weekRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val cal=Calendar.getInstance().apply{time=parseDate(selectedDate);set(Calendar.DAY_OF_WEEK,Calendar.MONDAY)}
        repeat(7){i->
            val d=dateString(cal.time);val label=SimpleDateFormat("EE\ndd",Locale("ru")).format(cal.time)
            val b=TextView(this).apply{text=label;gravity=Gravity.CENTER;textSize=11f;setTextColor(if(d==selectedDate)blue else white);background=rounded(if(d==selectedDate)panel2 else bg,10);setPadding(dp(2),dp(10),dp(2),dp(10));setOnClickListener{selectedDate=d;render()}}
            weekRow.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(3)});cal.add(Calendar.DAY_OF_MONTH,1)
        };week.addView(weekRow);content.addView(week)
        val list=card();addCardTitle(list,displayDate(selectedDate))
        val es=events(dayOfCalendar(selectedDate), selectedDate)
        if(es.isEmpty())list.addView(text("Нет событий. Добавь занятие или личное дело.",13,muted,false))
        es.forEach{list.addView(eventRow(it))}
        list.addView(button("＋ Добавить событие",blue){eventDialog()});content.addView(list)
        val template=card();addCardTitle(template,"Шаблон недели")
        val summary=listOf("Пн" to "Спортзал · Программирование · Логистика","Вт" to "Спортзал · Иностранный язык · Проектный практикум · Статистика","Ср" to "Работа · выходной от спортзала","Чт" to "Спортзал · Математическая экономика · Программная инженерия","Пт" to "Спортзал · Работа","Сб" to "Спортзал · Работа","Вс" to "Работа · выходной от спортзала")
        summary.forEach{(d,s)->template.addView(text("$d  $s",13,muted,false).apply{setPadding(0,dp(5),0,dp(5))})}
        content.addView(template)
    }
    private fun tasksPage(){
        addTitle("Задачи и привычки","Планируй дела и отмечай выполненное")
        val add=card();addCardTitle(add,"Новая задача");add.addView(button("＋ Создать задачу",blue){taskDialog()});content.addView(add)
        val habitsCard=card();addCardTitle(habitsCard,"Привычки")
        habits.filter{it.third.contains(dayOfCalendar(selectedDate))}.forEach{(name,time,_)->val cb=CheckBox(this).apply{text="$time  $name";textSize=14f;setTextColor(white);isChecked=habitDone(name,selectedDate);setOnCheckedChangeListener{_,v->setHabit(name,selectedDate,v)}};habitsCard.addView(cb)}
        habitsCard.addView(button("＋ Добавить привычку",panel2){habitDialog()});content.addView(habitsCard)
        val c=card();addCardTitle(c,"Все задачи")
        val ts=tasks().sortedWith(compareBy<Task>{it.done}.thenBy{it.date})
        if(ts.isEmpty())c.addView(text("Пока нет задач. Создай первую.",13,muted,false))
        ts.forEach{c.addView(taskRow(it))}
        content.addView(c)
        val subjectsCard=card();addCardTitle(subjectsCard,"Учебные предметы")
        subjects.forEach { s -> subjectsCard.addView(text("•  $s",14,white,false).apply { setPadding(0,dp(5),0,dp(5)) }) }
        content.addView(subjectsCard)
    }
    private fun gymPage(){
        addTitle("Спортзал","Тренировки и восстановление")
        val dow=dayOfCalendar(selectedDate);val planned=dow in listOf(1,2,4,5,6)
        val c=card();addCardTitle(c,if(planned)"День тренировки" else "День восстановления")
        c.addView(text(if(planned)"Сегодня можно записать упражнения и рабочие веса." else "Сегодня выходной от спортзала. Отдых тоже часть плана.",14,muted,false))
        c.addView(space(8));c.addView(button("＋ Записать тренировку",blue){workoutDialog()});content.addView(c)
        val history=card();addCardTitle(history,"История тренировок")
        val ws=workouts().sortedByDescending{it.date}
        if(ws.isEmpty())history.addView(text("Записей пока нет.",13,muted,false))
        ws.forEach{w->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            val col=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            col.addView(text("${w.date} · ${w.name}",14,white,true));col.addView(text(w.details.ifBlank{"Без подробностей"},12,muted,false))
            row.addView(col,LinearLayout.LayoutParams(0,-2,1f))
            row.addView(button("×",panel2){val all=workouts();all.remove(w);saveWorkouts(all);render()})
            history.addView(row);history.addView(space(8))
        };content.addView(history)
    }
    private fun progressPage(){
        addTitle("Прогресс","Результаты за всё время")
        val ts=tasks();val done=ts.count{it.done};val ws=workouts();val sl=sleeps()
        val c=card();addCardTitle(c,"Обзор")
        c.addView(text("Задачи выполнены: $done из ${ts.size}",15,white,true));c.addView(progressBar(if(ts.isEmpty())0 else done*100/ts.size,blue))
        c.addView(space(12));c.addView(text("Тренировки записаны: ${ws.size}",15,white,true))
        c.addView(space(12));c.addView(text("Записей сна: ${sl.size}",15,white,true))
        c.addView(space(12));c.addView(text("Цель сна: ${prefs.getFloat("sleep_target",9f)} ч",15,white,true))
        c.addView(button("＋ Записать сон",blue){sleepDialog()});content.addView(c)
        val sleepCard=card();addCardTitle(sleepCard,"История сна")
        if(sl.isEmpty())sleepCard.addView(text("Добавь время засыпания и пробуждения, чтобы отслеживать режим.",13,muted,false))
        sl.sortedByDescending{it.date}.forEach{s->sleepCard.addView(text("${s.date}  ${s.bed} → ${s.wake}",14,white,true));sleepCard.addView(text("${s.hours} ч · качество ${s.quality}/5",12,muted,false));sleepCard.addView(space(8))}
        content.addView(sleepCard)
    }
    private fun eventRow(e:Event):View{
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(8),0,dp(8));gravity=Gravity.CENTER_VERTICAL}
        val stripe=View(this).apply{background=rounded(when(e.category){"study"->blue;"work"->green;"gym"->yellow;else->purple},4)}
        row.addView(stripe,LinearLayout.LayoutParams(dp(4),dp(44)))
        row.addView(spaceWidth(10))
        val time=text("${e.start}\n${e.end}",12,muted,false);row.addView(time,LinearLayout.LayoutParams(dp(58),-2))
        val col=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        col.addView(text(e.title,14,white,true));col.addView(text((when(e.category){"study"->"Учёба";"work"->"Работа";"gym"->"Спортзал";else->"Личное"})+(if(e.room.isNotBlank())" · ауд. ${e.room}" else ""),12,muted,false))
        row.addView(col,LinearLayout.LayoutParams(0,-2,1f));return row
    }
    private fun taskRow(t:Task):View{
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(5),0,dp(5))}
        val cb=CheckBox(this).apply{isChecked=t.done;buttonTintList=android.content.res.ColorStateList.valueOf(green);setOnCheckedChangeListener{_,v->val all=tasks();all.find{it.id==t.id}?.done=v;saveTasks(all);render()}}
        row.addView(cb)
        val col=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        col.addView(text(t.title,14,if(t.done)muted else white,true))
        col.addView(text("${t.date.ifBlank{"Без срока"}} · ${if(t.priority=="high")"Высокий приоритет" else "Обычный приоритет"}",11,muted,false))
        row.addView(col,LinearLayout.LayoutParams(0,-2,1f))
        row.addView(button("✎",panel2){taskDialog(t)})
        row.addView(button("×",panel2){val all=tasks();all.removeAll{it.id==t.id};saveTasks(all);render()})
        return row
    }
    private fun taskDialog(existing:Task?=null){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        val title=EditText(this).apply{hint="Название задачи";setText(existing?.title?:"");setTextColor(white);setHintTextColor(muted)}
        val date=EditText(this).apply{hint="Дата YYYY-MM-DD";setText(existing?.date?:selectedDate);setTextColor(white);setHintTextColor(muted)}
        val priority=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Обычный приоритет","Высокий приоритет"));setSelection(if(existing?.priority=="high")1 else 0)}
        box.addView(title);box.addView(date);box.addView(priority)
        AlertDialog.Builder(this).setTitle(if(existing==null)"Новая задача" else "Изменить задачу").setView(box)
            .setNegativeButton("Отмена",null).setPositiveButton("Сохранить"){_,_->
                val name=title.text.toString().trim();if(name.isNotEmpty()){val all=tasks();val item=Task(existing?.id?:UUID.randomUUID().toString(),name,date.text.toString().trim().ifBlank{selectedDate},if(priority.selectedItemPosition==1)"high" else "normal",existing?.done?:false);if(existing!=null)all.removeAll{it.id==existing.id};all.add(item);saveTasks(all);render()}
            }.show()
    }
    private fun eventDialog(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        val name=EditText(this).apply{hint="Название события";setTextColor(white);setHintTextColor(muted)}
        val start=EditText(this).apply{hint="Начало, например 10:00";setTextColor(white);setHintTextColor(muted)}
        val end=EditText(this).apply{hint="Окончание, например 11:00";setTextColor(white);setHintTextColor(muted)}
        val category=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Личное","Учёба","Работа","Спортзал"))}
        box.addView(name);box.addView(start);box.addView(end);box.addView(category)
        AlertDialog.Builder(this).setTitle("Добавить событие").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Сохранить"){_,_->
            val n=name.text.toString().trim();if(n.isNotBlank()){val a=readArray("custom_events");a.put(JSONObject().put("date",selectedDate).put("title",n).put("start",start.text.toString()).put("end",end.text.toString()).put("category",listOf("personal","study","work","gym")[category.selectedItemPosition]));saveArray("custom_events",a);toast("Событие сохранено");render()}
        }.show()
    }
    private fun habitDialog(){
        val input=EditText(this).apply{hint="Название привычки";setTextColor(white);setHintTextColor(muted)}
        AlertDialog.Builder(this).setTitle("Добавить привычку").setView(input).setNegativeButton("Отмена",null).setPositiveButton("Добавить"){_,_->val n=input.text.toString().trim();if(n.isNotBlank()){val a=readArray("custom_habits");a.put(JSONObject().put("name",n));saveArray("custom_habits",a);Toast.makeText(this,"Привычка сохранена. Пользовательские привычки можно расширить в следующей версии.",Toast.LENGTH_LONG).show()}}.show()
    }
    private fun workoutDialog(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        val name=EditText(this).apply{hint="Название тренировки";setText("Основная тренировка");setTextColor(white);setHintTextColor(muted)}
        val details=EditText(this).apply{hint="Упражнения, вес, повторения";setTextColor(white);setHintTextColor(muted);minLines=3}
        box.addView(name);box.addView(details)
        AlertDialog.Builder(this).setTitle("Записать тренировку").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Сохранить"){_,_->val all=workouts();all.add(Workout(selectedDate,name.text.toString().ifBlank{"Тренировка"},details.text.toString()));saveWorkouts(all);render()}.show()
    }
    private fun sleepDialog(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        fun edit(hint:String,value:String):EditText=EditText(this).apply{this.hint=hint;setText(value);setTextColor(white);setHintTextColor(muted)}
        val bed=edit("Когда лёг, например 22:00","22:00");val wake=edit("Когда проснулся, например 07:00","07:00");val quality=edit("Качество сна от 1 до 5","3")
        box.addView(bed);box.addView(wake);box.addView(quality)
        AlertDialog.Builder(this).setTitle("Записать сон").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Сохранить"){_,_->
            try{val b=bed.text.toString().split(":").map{it.toInt()};val w=wake.text.toString().split(":").map{it.toInt()};var mins=(w[0]*60+w[1])-(b[0]*60+b[1]);if(mins<=0)mins+=1440;val all=sleeps();all.removeAll{it.date==selectedDate};all.add(Sleep(selectedDate,bed.text.toString(),wake.text.toString(),mins/60.0,(quality.text.toString().toIntOrNull()?:3).coerceIn(1,5)));saveSleeps(all);render()}catch(e:Exception){Toast.makeText(this,"Проверь формат времени",Toast.LENGTH_SHORT).show()}
        }.show()
    }
    private fun progressBar(pct:Int,color:Int):View{
        val outer=FrameLayout(this).apply{background=rounded(line,99)}
        val inner=View(this).apply{background=rounded(color,99)}
        outer.addView(inner,FrameLayout.LayoutParams((resources.displayMetrics.widthPixels- dp(60))*pct.coerceIn(0,100)/100,dp(7)))
        return outer
    }
    private fun backupDialog(){
        AlertDialog.Builder(this).setTitle("Локальные данные").setMessage("Данные приложения хранятся на этом телефоне в локальном хранилище Android. Резервное копирование системы может сохранить настройки приложения. Экспорт/импорт JSON добавим в следующем обновлении.")
            .setPositiveButton("Понятно",null).show()
    }
    private fun button(label:String,color:Int,onClick:()->Unit):TextView=TextView(this).apply{text=label;textSize=13f;gravity=Gravity.CENTER;setTextColor(if(color==blue)Color.rgb(16,32,57) else white);typeface=Typeface.DEFAULT_BOLD;background=rounded(color,12);setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener{onClick()}}
    private fun text(s:String,size:Int,color:Int,bold:Boolean):TextView=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(bold)typeface=Typeface.DEFAULT_BOLD}
    private fun space(h:Int):View=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(h))}
    private fun spaceWidth(w:Int):View=View(this).apply{layoutParams=LinearLayout.LayoutParams(dp(w),1)}
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int,r:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}

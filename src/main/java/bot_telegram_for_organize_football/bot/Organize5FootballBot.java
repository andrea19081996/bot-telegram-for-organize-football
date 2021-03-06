package bot_telegram_for_organize_football.bot;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.vdurmont.emoji.EmojiParser;

public class Organize5FootballBot extends TelegramLongPollingBot{

	private Map<Long,Set<String>> match= new HashMap<Long, Set<String>>();
	private Map<Long, Singol_match> setting_match = new HashMap<Long, Singol_match>();
	
	public String getBotUsername() {
		return "Organize5FootballBot";
	}
	
	public void onUpdateReceived(Update update) {
			
		if(update.hasMessage() && update.getMessage().hasText()) {
			
			String message_text= update.getMessage().getText().toLowerCase();
			message_text=message_text.replace("ì", "i");
			message_text=message_text.replace("í", "i");
			long chat_id= update.getMessage().getChatId();
			
			
			try {
				clean_data(chat_id);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			
			String user = update.getMessage().getFrom().getUserName();

			SendMessage message= new SendMessage();
			message.setChatId(chat_id);
			
			Set<String> current_match=match.get(chat_id);
			if (current_match==null) {
				current_match= new HashSet<String>();
			}
			
			Singol_match singol= setting_match.get(chat_id);
			
			if(is_a_date(message_text) && singol!=null && singol.isBool()) {
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");  
				LocalDateTime now = LocalDateTime.now();
				String date= dtf.format(now);
				
//				if(singol==null)
//					singol= new Singol_match(date, message_text);
//				else {
//					singol.setDate(date);
//					singol.setDate_time(message_text);
//				}
				
				singol.setDate(date);
				singol.setDate_time(message_text);
				singol.setBool(false);
				this.setting_match.put(chat_id, singol);
				this.match.remove(chat_id);
				message.setText("Impostata partita per " + message_text);
				execution(message);
				
			/*case booking of a person*/
			}else if (message_text.equals("non ci sono") || message_text.equals("assente") || message_text.contains("non ci sto")) {
				
				if(this.setting_match.get(chat_id)==null) {
					message.setText("Prima di inserire persone nella partita bisogna scegliere un giorno per la partita\nEsempio Lunedì 19:00");
					execution(message);
				} else {
					delete(chat_id, message, user, this.match, this.setting_match.get(chat_id).getDate_time());
					execution(message);
				}
				
			} else if(message_text.equals("ci sono") || message_text.equals("presente") || message_text.equals("ci sto")) {
				
				if(this.setting_match.get(chat_id)==null) {
					message.setText("Prima di inserire persone nella partita bisogna scegliere un giorno per la partita\nEsempio Lunedì 19:00\nUsa il comando /day_time per impostare una partita");
					execution(message);
				}else {
					booking(chat_id, message, user, this.match, this.setting_match.get(chat_id).getDate_time());
					execution(message);
				}
				
			} else if(message_text.equals("/list")) {
				
				if(this.setting_match.get(chat_id)==null) {
					message.setText("Prima bisogna scegliere un giorno per la partita\nEsempio Lunedì 19:00");
					execution(message);
				}else {
					String[] date_array= this.setting_match.get(chat_id).getDate_time().split(" ");
					message.setText("Partita per "+date_array[0]+" alle ore "+date_array[1]+": \n"+list_person_match(current_match));
					execution(message);
				}
				
			} else if(message_text.equals("/day_time")) { 
				
				message.setText("Scegli un giorno e un orario in cui far svolgere la partita\nScrivere GIORNO e ORA per impostare correttamente la partita\nEsempio: Lunedì 19:00");
				if(singol==null)
					singol= new Singol_match();
				else 
					singol.setBool(true);
				this.setting_match.put(chat_id, singol);
				execution(message);
				
			} else if(message_text.equals("/help")) { 
				
				String newMessage= "Questo bot ti aiuterà ad organizzare una partita 5vs5 :soccer: :\nAttraverso il comando /day_time potrai inserire il giorno in cui vuoi effettuare la partita\n"
						+ "Attraverso il comando /list potrai vedere le persone che si sono registrate per la partita da te creata\n";
				newMessage= EmojiParser.parseToUnicode(newMessage);
				message.setText(newMessage);
				execution(message);
				
			} 
			
		} 
		
	}

	public static void booking(Long chat_id, SendMessage message, String user, Map<Long,Set<String>> match, String date) {
		Set<String> current_match= match.get(chat_id);
		if(current_match==null)
			current_match= new HashSet<String>();
		if(!(current_match.contains(user)) && current_match.size()<10) {
			current_match.add(user);
			String[] date_array= date.split(" ");
			String match_person = "Partita per "+date_array[0]+" alle ore "+date_array[1]+": \n";
			String list_person_match= list_person_match(current_match);
			match_person= match_person.concat(list_person_match);
			if(current_match.size()==10) {
				match_person= match_person.concat("\nFormazione al completo :soccer:");

			}
			match.put(chat_id, current_match);
			match_person= EmojiParser.parseToUnicode(match_person);
			message.setText(match_person);
		} else {
			if(current_match.contains(user)) {
				message.setText(user + " già sei nella lista");
			}
			else
				message.setText(user + " per la partita già sono 10 sarà per la prossima");
		}
	}
	
	
	public void delete(Long chat_id, SendMessage message, String user, Map<Long,Set<String>> match, String date) {
		Set<String> current_match= match.get(chat_id);
		if(current_match.contains(user)) {
			current_match.remove(user);
			String[] date_array= date.split(" ");
			String match_person = "Partita per "+date_array[0]+" alle ore"+date_array[1]+": \n";
			String list_person_match= list_person_match(current_match);
			match_person= match_person.concat(list_person_match);
			message.setText(match_person);
			match.put(chat_id, current_match);
				
		} else {
			if(!current_match.contains(user))
				message.setText(user + " non sei nella lista");
		}
	}
	
	
	/*returns the list of person in the current_match*/
	public static String list_person_match(Set<String> current_match) {
		String list_person_match;
		Iterator<String> i= current_match.iterator();
		if(i.hasNext()) {
			list_person_match=i.next().concat("\n");
			while(i.hasNext()) {
				list_person_match= list_person_match.concat(i.next()+ "\n");
			}
			return list_person_match;
		}
		return "Non c'è ancora nessuno per la partita";
	}
	
	/*used for clean the old data*/
	public void clean_data(long chat_id) throws ParseException {
		boolean i;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");  
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));
		String date= dtf.format(now);
		Calendar calendar1= Calendar.getInstance();
		Date new_date= new SimpleDateFormat("yyyy/MM/dd").parse(date);
		calendar1.setTime(new_date);
		
		
		if(this.setting_match.get(chat_id)!=null && this.setting_match.get(chat_id).getDate()!=null) {
			Date old= new SimpleDateFormat("yyyy/MM/dd").parse(this.setting_match.get(chat_id).getDate());
			Calendar calendar2= Calendar.getInstance();
			calendar2.setTime(old);
			calendar2.add(Calendar.WEEK_OF_YEAR, +1);
			
			i= (((calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) && (calendar1.get(Calendar.WEEK_OF_YEAR) > calendar2.get(Calendar.WEEK_OF_YEAR))) || ((calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) && (calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR)) && (calendar1.get(Calendar.DAY_OF_YEAR) > calendar2.get(Calendar.DAY_OF_YEAR))) || (calendar1.get(Calendar.YEAR) > calendar2.get(Calendar.YEAR)));
			System.out.println(i);
			if (i==true) {
				if(this.match.get(chat_id)!=null)
					this.match.remove(chat_id);
				this.setting_match.remove(chat_id);
			}
		}
	}
	
	
	public void execution(SendMessage message) {
		
		try {
			execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
		
	}
	
	public static boolean is_a_date(String message) {
		String[] array_message= message.split(" ");
		if (array_message[0]== null || array_message.length>2)
			return false;
		
		if (is_a_day(array_message[0]) && is_a_time(array_message[1])) {
			return true;
		}
		return false;
		
	}
	
	
	public static boolean is_a_day(String day) {
		day=day.toLowerCase();
		if (day.equals("lunedi") || day.equals("martedi") || day.equals("mercoledi") || day.equals("giovedi") || day.equals("venerdi") || day.equals("sabato") || day.equals("domenica"))
			return true;
		else 
			return false;
	}
	
	public static boolean is_a_time(String time) {
		time=time.replace(" ", "");
		String[] array_time= time.split(":");
		int hours= Integer.parseInt(array_time[0]);
		int minute= Integer.parseInt(array_time[1]);
		
		if (hours<24 && hours>=00 && minute>=00 && minute<60)
			return true;
		else
			return false;
		
	}
	
	@Override
	public String getBotToken() {
		/*token of the bot */
		return System.getenv("HEROKU_AUX");
	}

}

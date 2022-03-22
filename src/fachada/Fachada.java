package fachada;
/**********************************
 * IFPB - Curso Superior de Tec. em Sist. para Internet
 * Persistência de Objetos
 * Prof. Fausto Maranhão Ayres
 * Grupo: João Jorge Evangelista Fernandes e João Pedro de Sousa Rodrigues
 * Novembro 2021
 **********************************/

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport; // send comentado a pedido do professor para fazer o teste.
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import dao.DAO;
import dao.DAOConvidado;
import dao.DAOParticipante;
import dao.DAOReuniao;
import modelo.Convidado;
import modelo.Participante;
import modelo.Reuniao;

public class Fachada {
	private static String emailOrigem;				//email de origem 
	private static String senhaOrigem;				//senha do email de origem
	private static boolean emailDesabilitado;		//desabilitar envio?
	private static DAOParticipante DAOParticipante = new DAOParticipante();
	private static DAOReuniao DAOReuniao = new DAOReuniao();
	private static DAOConvidado DAOConvidado = new DAOConvidado();

	public static void setEmailSenha(String email, String senha) {
		emailOrigem = email;
		senhaOrigem = senha;
	}
	public static void desabilitarEmail(boolean status) {
		emailDesabilitado = status;
	}

	public static void inicializar()  {
		DAO.open();
	}

	public static void	finalizar() {
		DAO.close();
	}


	public static Participante criarParticipante(String nome, String email) throws Exception {
		nome = nome.trim();
		email = email.trim();

		DAO.begin();

		Participante p =  DAOParticipante.read(nome); // Verifica se jÃ¡ existe participante com este nome
		if (p!=null){
			DAO.rollback();
			throw new Exception("Participante " + nome + " ja cadastrado(a)");
		}
		//Cadastrar participante na reuniï¿½o
		p = new Participante (nome, email);

		DAOParticipante.create(p);
		DAO.commit();
		return p;	
	}	

	public static Convidado criarConvidado(String nome, String email, String empresa) throws Exception{
		nome = nome.trim();
		email = email.trim();
		empresa = empresa.trim();

		DAO.begin();

		Participante p = DAOParticipante.read(nome);
		if (p!=null){
			DAO.rollback();
			throw new Exception("Participante " + nome + " ja cadastrado(a)");
		}
		//Cadastrar participante na reuniï¿½o
		Convidado conv = new Convidado(nome, email, empresa);

		DAOParticipante.create(conv);
		DAO.commit();
		return conv;	
	}	

	public static Reuniao criarReuniao (String datahora, String assunto, ArrayList<String> nomes) 
			throws Exception{
		assunto = assunto.trim();

		DAO.begin();

		LocalDateTime dth;
		try {
			DateTimeFormatter parser = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
			dth  = LocalDateTime.parse(datahora, parser); 
		}
		catch(DateTimeParseException e) {
			DAO.rollback();
			throw new Exception ("reuniao com formato de data invalido");
		}

		//Verificar o tamanho da lista de participantes se ï¿½ > 2
		if (nomes.size()<2) {
			DAO.rollback();
			throw new Exception ("Reuniao sem quorum minimo de dois participantes");
		}

		ArrayList<Participante> participantes = new ArrayList<>();
		for(String n : nomes) { 
			//Verificar se o participante existe
			Participante p =  DAOParticipante.read(n);
			if(p == null) {
				DAO.rollback();
				throw new Exception ("Participante " + n + " inexistente");
			}

			//Verificar se o participante jï¿½ estï¿½ em outra reuniï¿½o no mesmo horï¿½rio
			for (Reuniao r1 : p.getReunioes()) 	{
				Duration duracao = Duration.between(r1.getDatahora(), dth); //(d - hinicio)
				long horas = duracao.toHours();
				if(Math.abs(horas) < 2) {
					DAO.rollback();
					throw new Exception("Participante ja esta em outra reuniao nesse horario");
				}
			}
			participantes.add(p);
		}
		Reuniao r = new Reuniao(dth, assunto);	
		
		//relacionar participante e reuniao 
		for(Participante p : participantes)	{
			r.adicionar(p);
			p.adicionar(r);
		}

		DAOReuniao.create(r);
		DAO.commit();

		//enviar email para participantes
		for(Participante p : participantes)	
			enviarEmail(p.getEmail(), "nova reuniao", "Voce foi agendado para a reuniao na data:"+r.getDatahora()+" e assunto:"+assunto);

		return r;
	}

	public static void 	adicionarParticipanteReuniao(String nome, int id) throws Exception 	{
		nome = nome.trim();

		DAO.begin();
		//inicio da transacao
		//Verificar se o participante existe
		Participante p = DAOParticipante.read(nome);//localizarParticipante
		if(p == null) {
			DAO.rollback();
			throw new Exception("Participante " + nome + " nao consta no cadastro");
		}

		//Verificar de a reuniaï¿½o existe no repositï¿½rio
		Reuniao r = DAOReuniao.read(id);//localizarReuniao
		if(r == null) {
			DAO.rollback();
			throw new Exception("Reuniao " + id + " nao cadastrada");
		}

		//Verificar se o participante jï¿½ participa desta reuniï¿½o
		if(r.localizarParticipante(nome) == p) {
			DAO.rollback();
			throw new Exception("Participante " + nome + " ja cadastrado na reuniao " + id);
		}

		//Verificar se o participante jï¿½ estï¿½ em outra reuniï¿½o no mesmo horï¿½rio
		for (Reuniao r1 : p.getReunioes()) 	{
			Duration duracao = Duration.between(r1.getDatahora(), r.getDatahora());
			long horas = duracao.toHours();
			if(Math.abs(horas) < 2) {
				DAO.rollback();
				throw new Exception("Participante ja esta em outra reuniao nesse horario");
			}
		}

		//Adicionar o participante na reuniï¿½o e vice-versa
		r.adicionar(p);
		p.adicionar(r);

		DAOReuniao.update(r);
		DAOParticipante.update(p);
		DAO.commit();

		//enviar email para o novo participante
		enviarEmail(p.getEmail(), "novo participante", "Voce foi adicionado a reuniao na data:"+r.getDatahora()+" e assunto:"+r.getAssunto());

	}

	public static void 	removerParticipanteReuniao(String nome, int id) throws Exception {
		nome = nome.trim();

		DAO.begin();

		//Verificar se o participante existe
		Participante p = DAOParticipante.read(nome); //localizarParticipante
		if(p == null) {
			DAO.rollback();
			throw new Exception("Participante " + nome + " nao consta no cadastro");
		}

		//Verificar se a reuniï¿½o estï¿½ cadastrada
		Reuniao r = DAOReuniao.read(id);//localizarReuniao
		if(r == null) {
			DAO.rollback();
			throw new Exception("Reuniao " + id + " nao cadastrada");
		}

		//Remover participante da reuniï¿½o 
		r.remover(p);
		p.remover(r);

		DAOReuniao.update(r);
		DAOParticipante.update(p);
		DAO.commit();

		//enviar email para o  participante removido
		enviarEmail(p.getEmail(), "participante removido", "Voce foi removido da reuniao na data:"+r.getDatahora()+" e assunto:"+r.getAssunto());

		//Cancelar a reuniï¿½o por falta de quï¿½rum mï¿½nimo de 2 participantes
		if (r.getTotalParticipantes() < 2) 
			cancelarReuniao(id);
	}

	public static void	cancelarReuniao(int id) throws Exception	{
		DAO.begin();

		Reuniao r = DAOReuniao.read(id);
		if (r == null) {
			DAO.rollback();
			throw new Exception("Reuniao " + id + " nao cadastrada");
		}
		
		for (Participante p : r.getParticipantes()) {
			p.remover(r);
			r.remover(p);
			DAOParticipante.update(p);
		}
		
		DAOReuniao.delete(r);
		DAO.commit();
		
		//enviar email para todos os participantes
		for (Participante p : r.getParticipantes()) 
			enviarEmail(p.getEmail(), "reuniao cancelada", "data:+"+r.getDatahora()+" e assunto:"+r.getAssunto());

	}

	public static void apagarParticipante(String nome) throws Exception {
		nome = nome.trim();
		Participante p = DAOParticipante.read(nome);
		if (p==null) {
			DAO.rollback();
			throw new Exception("Participante " + nome + " nao cadastrado(a)");
		}
		if (p.getReunioes().size() > 1) {
			for (Reuniao r : p.getReunioes()){
				DAO.begin();
				r.remover(p);
				DAOReuniao.update(r);
				DAO.begin();
			
			DAO.begin();
			DAOParticipante.delete(p);
			DAO.commit();
			}
		} else {
			DAO.begin();
			for (Reuniao r : p.getReunioes()){
				r.remover(p);
				DAOReuniao.update(r);
			}
			DAOParticipante.delete(p);
			DAO.commit();
		}

		for (Reuniao r : p.getReunioes()) {
			if (r.getParticipantes().size() < 2)
				cancelarReuniao(r.getId());
		}
		//enviar email para o participante apagado
		enviarEmail(p.getEmail()," descadastro ",  "Voce foi excluido da agenda");
	}	

	public static List<Participante> listarParticipantes() { 
		List<Participante> result = DAOParticipante.readAll();
		return result;
		}

	public static List<Convidado> listarConvidados() { 
		List<Convidado> result = DAOConvidado.readAll();
		return result;
		}

	public static List<Reuniao> listarReunioes() { 
		List<Reuniao> result = DAOReuniao.readAll();
		return result; 
		}

	public static List<Participante> consultaA(String nome, int mes) { return DAOParticipante.consultarParticipante(nome, mes); }

	public static List<Reuniao> consultaB() { return DAOReuniao.consultarConvidado(); }

	/*
	 * ********************************************************
	 * Obs: lembrar de desligar antivirus e 
	 * de ativar "Acesso a App menos seguro" na conta do gmail
	 * 
	 * biblioteca java.mail 1.6.2
	 * ********************************************************
	 */
	public static void enviarEmail(String emaildestino, String assunto, String mensagem) {
		try {
			if (Fachada.emailDesabilitado)
				return;

			String emailorigem = Fachada.emailOrigem;
			String senhaorigem = Fachada.senhaOrigem;

			//configurar email de origem
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");
			Session session;
			session = Session.getInstance(props,
					new javax.mail.Authenticator() 	{
				protected PasswordAuthentication getPasswordAuthentication() 	{
					return new PasswordAuthentication(emailorigem, senhaorigem);
				}
			});

			//criar e enviar email
			MimeMessage message = new MimeMessage(session);
			message.setSubject(assunto);
			message.setFrom(new InternetAddress(emailorigem));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emaildestino));
			message.setText(mensagem);   // usar "\n" para quebrar linhas
			Transport.send(message);
		} 
		catch (MessagingException e) {
			System.out.println(e.getMessage());
		} 
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

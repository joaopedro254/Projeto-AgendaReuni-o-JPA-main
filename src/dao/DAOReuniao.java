package dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import modelo.Participante;
import modelo.Reuniao;

public class DAOReuniao extends DAO<Reuniao> {

	@Override
	public Reuniao read(Object chave) {
		try {
			int id = (int) chave;
			TypedQuery<Reuniao> q = manager.createQuery("Select r from Reuniao r where r.id=:n", Reuniao.class);
			q.setParameter("n", id);
			return q.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}	
	}
	
	@Override
	public List<Reuniao> readAll(){
		TypedQuery<Reuniao> q = manager.createQuery("Select r from Reuniao r order by r.id", Reuniao.class);
		return q.getResultList();
	}

	public List<Reuniao> consultarConvidado() {
		try {
			TypedQuery<Reuniao> q = manager.createQuery (
					"SELECT distinct r " +
					"FROM Convidado c JOIN c.reunioes r ",Reuniao.class);
			return q.getResultList();
		} catch(Exception e){
			return null;
		}
	}

}



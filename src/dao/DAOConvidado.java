package dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.mysql.cj.QueryReturnType;

import modelo.Convidado;
import modelo.Participante;

public class DAOConvidado extends DAO<Convidado> {

	@Override
	public Convidado read(Object chave) {
		try {
			String nome = (String) chave;
			TypedQuery<Convidado> q = manager.createQuery("Select c from Convidado c where c.nome=:n", Convidado.class);
			q.setParameter("n", nome);
			return q.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}	
	}
	
	@Override
	public List<Convidado> readAll(){
		TypedQuery<Convidado> q = manager.createQuery("Select c from Convidado c order by c.id", Convidado.class);
		return q.getResultList();
	}

}

package com.husa.exostar.server.data;

import com.husa.exostar.server.data.record.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseDAO {

  @Autowired UsersRepository usersRepository;

  public Users createOrUpdateUsers(Users record) {
    return usersRepository.save(record);
  }

  public List<Users> getAllUsers() {
    return usersRepository.findAll();
  }
}

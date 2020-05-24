// package com.ranjay.bootstrap;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;

// import java.util.List;

// import com.ranjay.bootstrap.config.RootConfig;
// import com.ranjay.bootstrap.model.Task;
// import com.ranjay.bootstrap.model.User;
// import com.ranjay.bootstrap.service.TaskService;
// import com.ranjay.bootstrap.service.UserService;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit.jupiter.SpringExtension;

// /**
//  * InnerNhSystemApplicationTest
//  */
// @ExtendWith(SpringExtension.class)
// @ContextConfiguration(classes = {RootConfig.class})
// public class NhSystemApplicationTest {

//     @Autowired
//     private UserService userService;

//     @Autowired
//     private TaskService taskService;

//     @BeforeEach
//     public void initDb() {
//         {
//             User newUser = new User("testuser@gmail.com", "testUser", "password");
//             userService.createUser(newUser);
//         }
//         {
//             User newUser = new User("testAdmin@gmail.com", "testAdmin", "password");
//             userService.createUser(newUser);
//         }
//         Task userTask = new Task("03/02/2020", "00:11", "11:00", "you need to work now");
//         User user = userService.findOne("testuser@gmail.com");
//         taskService.addTask(userTask, user);
//     }

//     @Test
//     public void testUser(){
//         User user = userService.findOne("testuser@gmail.com");
//         assertNotNull(user);
//         User admin = userService.findOne("testAdmin@gmail.com");
//         assertEquals(admin.getEmail(), "testAdmin@gmail.com");
//     }

//     @Test
//     public void testTask(){
//         User user = userService.findOne("testuser@gmail.com");
//         List<Task> tasks = taskService.findUserTask(user);
//         assertNotNull(tasks);
//     }

// }
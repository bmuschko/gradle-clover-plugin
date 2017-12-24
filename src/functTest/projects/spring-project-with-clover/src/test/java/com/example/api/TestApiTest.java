package com.example.api;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.SecurityApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SecurityApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("junit")
public class TestApiTest {

    @Rule
    public ExpectedException      thrown = ExpectedException.none();

    @Autowired
    private WebApplicationContext context;

    private MockMvc               mvc;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(username = "1", authorities = "default")
    public void test() throws Exception {
        // given:
        Long id = 1L;

        // when:
        this.mvc.perform(get("/test/{id}/hello", id).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello World!")));
    }

    @Test
    @WithMockUser(username = "2", authorities = "default")
    public void hacker() throws Exception {
        // given:
        Long id = 1L;

        thrown.expectCause(instanceOf(AccessDeniedException.class));

        // when:
        this.mvc.perform(get("/test/{id}/hello", id).accept(MediaType.APPLICATION_JSON));
    }

}

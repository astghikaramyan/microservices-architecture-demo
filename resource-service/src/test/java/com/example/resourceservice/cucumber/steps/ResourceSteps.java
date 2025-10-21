package com.example.resourceservice.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.resourceservice.controller.ResourceRestController;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.repository.OutboxEventRepository;
import com.example.resourceservice.repository.ResourceRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ResourceSteps {

    @Autowired
    private ResourceRestController resourceRestController;

    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private MockMvc mockMvc;

    private byte[] resourceData;
    private ResourceEntity storedResource;

    @Given("I have a resource file")
    public void i_have_a_resource_file() {
        resourceData = "dummy audio".getBytes();
        mockMvc = MockMvcBuilders.standaloneSetup(resourceRestController).build();
    }

    @When("I POST the resource to {string}")
    public void i_post_the_resource_to(String url) throws Exception {
        String response = mockMvc.perform(post(url)
                        .content(resourceData)
                        .contentType(MediaType.valueOf("audio/mpeg")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<ResourceEntity> resources = resourceRepository.findAll();
        storedResource = resources.get(0);
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        assertThat(status).isEqualTo(200); // simple check
    }

    @Then("the resource should be stored in the database")
    public void the_resource_should_be_stored_in_the_database() {
        assertThat(resourceRepository.findById(storedResource.getId())).isPresent();
    }

    @Given("a resource exists in the database")
    public void a_resource_exists_in_the_database() {
        ResourceEntity entity = new ResourceEntity();
        entity.setFileName("test file");
        entity.setS3Key("s3Key");
        entity.setUploadedAt(LocalDateTime.now());
        storedResource = resourceRepository.save(entity);
        mockMvc = MockMvcBuilders.standaloneSetup(resourceRestController).build();
    }


    @When("I GET the resource by its ID")
    public void i_get_the_resource_by_its_id() throws Exception {
        mockMvc.perform(get("/resources/{id}", storedResource.getId())
                        .accept(MediaType.valueOf("audio/mpeg")))
                .andExpect(status().isOk());
    }

    @Then("the response should contain the resource file name")
    public void the_response_should_contain_the_resource_file_name() {
        ResourceEntity found = resourceRepository.findById(storedResource.getId()).orElseThrow();
        assertThat(found.getFileName()).isEqualTo("test file");
    }

    @When("I DELETE the resource by its ID")
    public void i_delete_the_resource_by_its_id() throws Exception {
        mockMvc.perform(delete("/resources")
                        .param("id", storedResource.getId().toString()))
                .andExpect(status().isOk());
    }

    @Then("the resource should be removed from the database")
    public void the_resource_should_be_removed_from_the_database() {
        assertThat(resourceRepository.findById(storedResource.getId())).isEmpty();
    }
}

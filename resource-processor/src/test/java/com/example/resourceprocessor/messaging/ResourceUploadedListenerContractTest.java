package com.example.resourceprocessor.messaging;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.example.resourceprocessor.client.ResourceServiceClient;
import com.example.resourceprocessor.client.SongServiceClient;
import com.example.resourceprocessor.messaging.consumer.CreateResourceMetadataListener;
import com.example.resourceprocessor.messaging.publisher.ProcessSongMetadataPublisher;
import com.example.resourceprocessor.model.SongMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(MockitoExtension.class)
@PactTestFor(providerName = "resource-service-queue", providerType = ProviderType.ASYNCH)
public class ResourceUploadedListenerContractTest {

    private static final String RESOURCE_ID = "42";

    private static final byte[] SAMPLE_RESOURCE_DATA = "mock audio data".getBytes();

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private ResourceServiceClient resourceServiceClient;
    @Mock
    private ProcessSongMetadataPublisher processSongMetadataPublisher;


    @InjectMocks
    private CreateResourceMetadataListener resourceUploadedListener;

    @Pact(consumer = "resource-processor", provider = "resource-service")
    public V4Pact resourceUploadedMessagePact(PactBuilder builder) {
        return builder
                .usingLegacyMessageDsl()
                .expectsToReceive("a resource uploaded event")
                .withContent(RESOURCE_ID)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "resourceUploadedMessagePact")
    void testResourceUploadedProcessing(List<V4Interaction.AsynchronousMessage> messages) {
        String message = new String(messages.get(0).contentsAsBytes());
        byte[] mockResponse = SAMPLE_RESOURCE_DATA;
        when(resourceServiceClient.getResourceBinary(RESOURCE_ID)).thenReturn(mockResponse);

        resourceUploadedListener.createResourceMetadata().accept(MessageBuilder.withPayload(message).build());

        verify(resourceServiceClient).getResourceBinary(RESOURCE_ID);
        verify(songServiceClient).saveResourceMetadata(any(SongMetadata.class));
    }
}
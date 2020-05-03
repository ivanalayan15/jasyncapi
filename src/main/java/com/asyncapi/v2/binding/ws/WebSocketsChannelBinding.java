package com.asyncapi.v2.binding.ws;

import com.asyncapi.v2.binding.ChannelBinding;
import lombok.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * This class MUST NOT contain any properties. Its name is reserved for future use.
 *
 * This class defines how to describe WebSockets channel binding.
 *
 * @author Pavel Bodiachevskii
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebSocketsChannelBinding extends ChannelBinding {

    /**
     * The HTTP method to use when establishing the connection. Its value MUST be either GET or POST.
     */
    @Nullable
    @CheckForNull
    private String method;

    /**
     * A Schema object containing the definitions for each query parameter. This schema MUST be of type
     * object and have a properties key.
     *
     * @see <a href="https://www.asyncapi.com/docs/specifications/2.0.0/#schemaObject">Schema object</a>
     */
    @Nullable
    @CheckForNull
    private Object query;

    /**
     * A Schema object containing the definitions of the HTTP headers to use when establishing the connection.
     * This schema MUST be of type object and have a properties key.
     *
     * @see <a href="https://www.asyncapi.com/docs/specifications/2.0.0/#schemaObject">Schema object</a>
     */
    @Nullable
    @CheckForNull
    private Object headers;

    /**
     * The version of this binding. If omitted, "latest" MUST be assumed.
     */
    @Nullable
    @CheckForNull
    private String bindingVersion;

}

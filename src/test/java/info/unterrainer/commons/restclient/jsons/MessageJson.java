package info.unterrainer.commons.restclient.jsons;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class MessageJson {

	private String message;
}

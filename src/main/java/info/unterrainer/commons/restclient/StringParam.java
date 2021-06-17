package info.unterrainer.commons.restclient;

import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class StringParam {

	@Singular
	private Map<String, String> parameters;
}

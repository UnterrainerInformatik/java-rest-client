package info.unterrainer.commons.restclient.jsons;

import info.unterrainer.commons.serialization.jsons.BasicJson;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class EliteUserJson extends BasicJson {

	private String userName;
	private String client;

	private String givenName;
	private String familyName;

	private String email;
	private Boolean emailVerified;

	private String realmRoles;
	private String clientRoles;
	private String readTenants;
	private String writeTenants;
	private Long valueTenant;

	private Boolean isActive;
	private Boolean isBearer;
}

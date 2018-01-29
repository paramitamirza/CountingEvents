
public class Participant {
	
	private String firstName;
	private String middleName;
	private String lastName;
	private String fullName;
	
	private String role;
	
	public Participant() {
		
	}
	
	public Participant(String firstName, String middleName, String lastName) {
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = this.firstName + " " + this.lastName;
	}
	
	public Participant(String firstName, String middleName, String lastName, String role) {
		this(firstName, middleName, lastName);
		this.role = role;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	
	

}

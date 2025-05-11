package dto;

public class ProfileDTO {
    private String username;
    private String displayName;
    private String profilePicture;

    public ProfileDTO(String username, String displayName, String profilePicture) {
        this.username = username;
        this.displayName = displayName;
        this.profilePicture = profilePicture;
    }
}

package pk.mr.pasir_rozek_mateusz.dto;

public record GroupExpenseNotification(
        String type,
        Long groupId,
        String groupName,
        String title,
        Double amount,
        Double userShare,
        String createdByEmail,
        String message
) {
    public GroupExpenseNotification(Long groupId, String groupName, String title,
                                    Double amount, Double userShare, String createdByEmail,
                                    String message) {
        this("GROUP_EXPENSE_ADDED", groupId, groupName, title, amount, userShare, createdByEmail, message);
    }
}

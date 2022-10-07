# HideyLink
HideySMP whitelisting and permission management plugin

### Requirements

- Paper 1.19.2
- [LuckPerms](https://luckperms.net/)

### Configuration
```yaml
auth:
  guild: 264801645370671114
  role: 426156903555399680
  moderator: 863124017064706069
  supporter: 743861104819830854
```
- `auth.guild` - Discord server ID
- `auth.role` - Role ID required to join
- `auth.moderator` - Text channel ID for moderators
- `auth.supporter` - Text channel ID for supporters

> Keep in mind that In all properties where we use text channels the player will be checked for the ability to talk

### Groups and Permissions

Plugin will automatically assign following groups to players (if required):

- `group.moderator` - Group given to server staff
- `group.supporter` - Group given to supporters

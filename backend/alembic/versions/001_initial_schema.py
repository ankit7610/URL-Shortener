"""Initial database schema

Revision ID: 001
Revises: 
Create Date: 2026-01-07

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '001'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create users table
    op.create_table(
        'users',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('email', sa.String(length=255), nullable=False),
        sa.Column('hashed_password', sa.String(length=255), nullable=False),
        sa.Column('full_name', sa.String(length=255), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('is_verified', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('is_admin', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('api_key', sa.String(length=64), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('last_login', sa.DateTime(), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_user_email', 'users', ['email'], unique=True)
    op.create_index('idx_user_api_key', 'users', ['api_key'], unique=True)
    
    # Create urls table
    op.create_table(
        'urls',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('short_code', sa.String(length=50), nullable=False),
        sa.Column('original_url', sa.Text(), nullable=False),
        sa.Column('custom_alias', sa.String(length=100), nullable=True),
        sa.Column('title', sa.String(length=255), nullable=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('user_id', sa.Integer(), nullable=True),
        sa.Column('password_hash', sa.String(length=255), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('expires_at', sa.DateTime(), nullable=True),
        sa.Column('click_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('last_accessed', sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_url_short_code', 'urls', ['short_code'], unique=True)
    op.create_index('idx_url_custom_alias', 'urls', ['custom_alias'], unique=True)
    op.create_index('idx_url_user_id', 'urls', ['user_id'])
    op.create_index('idx_url_created_at', 'urls', ['created_at'])
    op.create_index('idx_url_user_created', 'urls', ['user_id', 'created_at'])
    
    # Create analytics table
    op.create_table(
        'analytics',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('url_id', sa.Integer(), nullable=False),
        sa.Column('clicked_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('country', sa.String(length=100), nullable=True),
        sa.Column('city', sa.String(length=100), nullable=True),
        sa.Column('device_type', sa.String(length=50), nullable=True),
        sa.Column('browser', sa.String(length=50), nullable=True),
        sa.Column('os', sa.String(length=50), nullable=True),
        sa.Column('referrer', sa.String(length=500), nullable=True),
        sa.Column('user_agent', sa.String(length=500), nullable=True),
        sa.ForeignKeyConstraint(['url_id'], ['urls.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_analytics_url_id', 'analytics', ['url_id'])
    op.create_index('idx_analytics_clicked_at', 'analytics', ['clicked_at'])
    op.create_index('idx_analytics_url_time', 'analytics', ['url_id', 'clicked_at'])
    op.create_index('idx_analytics_country', 'analytics', ['country'])
    op.create_index('idx_analytics_device', 'analytics', ['device_type'])


def downgrade() -> None:
    op.drop_table('analytics')
    op.drop_table('urls')
    op.drop_table('users')
